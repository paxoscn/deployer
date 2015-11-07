package cn.paxos.deployer;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class PluginContext
{

  public static final Map<String, Object> attributes = new HashMap<String, Object>();

  private final LifecycleListener lifecycleListener;
  private final Map<String, Plugin> plugins;

  public PluginContext(String pluginsFolder, LifecycleListener lifecycleListener) throws IOException, InterruptedException
  {
    this.lifecycleListener = lifecycleListener;
    this.plugins = new HashMap<String, Plugin>();
    if (pluginsFolder == null || lifecycleListener == null)
    {
      throw new IllegalArgumentException("pluginsFolder or lifecycleListener can not be null");
    }
    for (File file : new File(pluginsFolder).listFiles(new FileFilter()
    {
      @Override
      public boolean accept(File file)
      {
        return filter(file);
      }
    }))
    {
      deploy(file);
    }
    WatchService watchService = FileSystems.getDefault().newWatchService();
    Paths.get(pluginsFolder).register(watchService,
        StandardWatchEventKinds.ENTRY_CREATE,
        StandardWatchEventKinds.ENTRY_MODIFY,
        StandardWatchEventKinds.ENTRY_DELETE);
    // Sometime creating a file would raise multiple events, one CREATE and one MODIFY ...
    Set<String> handled = new HashSet<String>();
    while (true)
    {
      WatchKey key = watchService.take();
      for (WatchEvent<?> event : key.pollEvents())
      {
        WatchEvent.Kind<?> kind = event.kind();
        if (kind == StandardWatchEventKinds.OVERFLOW)
        {
          continue;
        }
        Path filename = (Path) event.context();
        File file = Paths.get(pluginsFolder).resolve(filename).toFile();
        if (handled.contains(file.getCanonicalPath() + " " + file.length()) || !filter(file))
        {
          continue;
        }
        if (kind == StandardWatchEventKinds.ENTRY_CREATE
            || kind == StandardWatchEventKinds.ENTRY_MODIFY)
        {
          deploy(file);
        } else if (kind == StandardWatchEventKinds.ENTRY_DELETE)
        {
          delete(file);
        }
        handled.add(file.getCanonicalPath() + " " + file.length());
      }
      key.reset();
    }
  }

  private boolean filter(File file)
  {
    return (!file.isDirectory()) && file.getName().endsWith(".par");
  }

  private void deploy(final File file)
  {
    try
    {
      System.out.println("Extracting " + file.getAbsolutePath());
      File pluginFolder = extract(file);
      System.out.println("Extracted to " + pluginFolder.getAbsolutePath());
      File ini = new File(pluginFolder, "plugin.ini");
      File classesFolder = new File(pluginFolder, "classes");
      File libFolder = new File(pluginFolder, "lib");
      validateFiles(ini, classesFolder, libFolder);
      Properties props = new Properties();
      FileInputStream iniInputStream = new FileInputStream(ini); 
      props.load(iniInputStream);
      iniInputStream.close();
      String pluginNamespace = validateAndGet(props, "namespace");
      String pluginName = validateAndGet(props, "name");
      String pluginVersion = validateAndGet(props, "version");
      final String pluginQN = getQN(pluginNamespace, pluginName);
      Plugin existingPlugin = plugins.get(pluginQN);
      if (existingPlugin != null)
      {
        System.out.println("Deleting plugin " + pluginQN + " " + existingPlugin.getVersion());
        lifecycleListener.onDelete(existingPlugin);
        System.out.println("Deleted");
      }
      System.out.println("Deploying plugin " + pluginQN + " " + pluginVersion);
      List<URL> urls = new LinkedList<URL>();
      if (classesFolder.exists())
      {
        urls.add(toURL(classesFolder));
      }
      if (libFolder.exists())
      {
        for (File jar : libFolder.listFiles(new FileFilter()
        {
          @Override
          public boolean accept(File file)
          {
            return (!file.isDirectory()) && file.getName().endsWith(".jar");
          }
        }))
        {
          urls.add(toURL(jar));
        }
      }
      // TODO close
      final ClassLoader cl = new OverridingClassLoader(urls.toArray(new URL[0]), Thread.currentThread().getContextClassLoader());
      final Class<?> pluginMainClass = cl.loadClass(validateAndGet(props, "main"));
      final Plugin plugin = new Plugin(pluginNamespace, pluginName, pluginVersion, file.getAbsolutePath(), cl, props);
      new Thread() {
        @Override
        public void run()
        {
          Thread.currentThread().setContextClassLoader(cl);
          try
          {
            pluginMainClass.getMethod("main", new Class<?>[] { String[].class }).invoke(null, new Object[] { new String[0] });
          } catch (Exception e)
          {
            System.err.println("Failed to deploy " + file.getAbsolutePath());
            System.err.println("Exception:" + e.getClass() + " - " + e.getMessage());
            e.printStackTrace();
          }
          plugins.put(pluginQN, plugin);
          lifecycleListener.onDeploy(plugin);
          System.out.println("Deployed");
        }
      }.start();
    } catch (Exception e)
    {
      System.err.println("Failed to deploy " + file.getAbsolutePath());
      System.err.println("Exception:" + e.getClass() + " - " + e.getMessage());
      e.printStackTrace();
    }
  }

  private void delete(File file)
  {
    for (Plugin plugin : plugins.values())
    {
      if (plugin.getPath().equals(file.getAbsolutePath()))
      {
        String pluginQN = getQN(plugin.getNamespace(), plugin.getName());
        System.out.println("Deleting plugin " + pluginQN + " " + plugin.getVersion());
        lifecycleListener.onDelete(plugin);
        System.out.println("Deleted");
        return;
      }
    }
  }

  private File extract(File file) throws IOException
  {
    File pluginFolder = Files.createTempDirectory("deployer").toFile();
    ZipInputStream zis = new ZipInputStream(new FileInputStream(file));
    ZipEntry ze = zis.getNextEntry();
    byte[] buffer = new byte[4096];
    while (ze != null)
    {
      String fileName = ze.getName();
      File newFile = new File(pluginFolder, fileName);
      new File(newFile.getParent()).mkdirs();
      if (ze.isDirectory())
      {
        new File(ze.getName()).mkdir();
      } else
      {
        FileOutputStream fos = new FileOutputStream(newFile);
        int len;
        while ((len = zis.read(buffer)) > 0)
        {
          fos.write(buffer, 0, len);
        }
        fos.close();
      }
      ze = zis.getNextEntry();
    }
    zis.closeEntry();
    zis.close();
    return pluginFolder;
  }

  private void validateFiles(File ini, File classesFolder, File libFolder) throws IOException
  {
    if ((!ini.exists()) || ini.isDirectory())
    {
      throw new IOException("plugin.ini does not exist or is not a valid file");
    }
    if (classesFolder.exists() && !classesFolder.isDirectory())
    {
      throw new IOException("classes folder is not a valid folder");
    }
    if (libFolder.exists() && !libFolder.isDirectory())
    {
      throw new IOException("lib folder is not a valid folder");
    }
    if ((!classesFolder.exists()) && (!libFolder.exists()))
    {
      throw new IOException("either a classes folder or a lib folder must exist");
    }
  }

  private String validateAndGet(Properties props, String key) throws IOException
  {
    String value = props.getProperty(key);
    if (value == null)
    {
      throw new IOException(key + " unspecified");
    }
    value = value.trim();
    if (value.length() < 1)
    {
      throw new IOException(key + " blank");
    }
    return value;
  }

  private String getQN(String pluginNamespace, String pluginName)
  {
    return pluginNamespace + "." + pluginName;
  }
  
  private URL toURL(File f) throws IOException
  {
    String path = f.getCanonicalPath().replace('\\', '/');
    if (!path.startsWith("/"))
    {
      path = "/" + path;
    }
    if (f.isDirectory() && (!path.endsWith("/")))
    {
      path += "/";
    }
    path = "file:" + path;
    return new URL(path);
  }

}
