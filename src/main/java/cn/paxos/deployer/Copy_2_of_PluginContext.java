//package cn.paxos.deployer;
//
//import java.io.File;
//import java.io.FileFilter;
//import java.io.FileInputStream;
//import java.io.FileOutputStream;
//import java.io.IOException;
//import java.nio.file.FileSystems;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.nio.file.Paths;
//import java.nio.file.StandardWatchEventKinds;
//import java.nio.file.WatchEvent;
//import java.nio.file.WatchKey;
//import java.nio.file.WatchService;
//import java.util.Arrays;
//import java.util.HashMap;
//import java.util.LinkedList;
//import java.util.List;
//import java.util.Map;
//import java.util.Properties;
//import java.util.zip.ZipEntry;
//import java.util.zip.ZipInputStream;
//
//public class Copy_2_of_PluginContext
//{
//
//  private final List<LifecycleListener> lifecycleListeners;
//  private final Map<String, Plugin> plugins;
//
//  public Copy_2_of_PluginContext(String pluginsFolder, LifecycleListener... lifecycleListeners) throws IOException, InterruptedException
//  {
//    this.lifecycleListeners = new LinkedList<LifecycleListener>(Arrays.asList(lifecycleListeners));
//    this.plugins = new HashMap<String, Plugin>();
//    if (pluginsFolder == null)
//    {
//      throw new IllegalArgumentException("pluginsFolder or lifecycleListener can not be null");
//    }
//    for (File file : new File(pluginsFolder).listFiles(new FileFilter()
//    {
//      @Override
//      public boolean accept(File file)
//      {
//        return filter(file);
//      }
//    }))
//    {
//      deploy(file);
//    }
//    WatchService watchService = FileSystems.getDefault().newWatchService();
//    Paths.get(pluginsFolder).register(watchService,
//        StandardWatchEventKinds.ENTRY_CREATE,
//        StandardWatchEventKinds.ENTRY_MODIFY,
//        StandardWatchEventKinds.ENTRY_DELETE);
//    while (true)
//    {
//      WatchKey key = watchService.take();
//      // Sometime creating a file would raise multiple events, one CREATE and one MODIFY ...
//      List<File> handled = new LinkedList<File>();
//      for (WatchEvent<?> event : key.pollEvents())
//      {
//        WatchEvent.Kind<?> kind = event.kind();
//        if (kind == StandardWatchEventKinds.OVERFLOW)
//        {
//          continue;
//        }
//        Path filename = (Path) event.context();
//        File file = Paths.get(pluginsFolder).resolve(filename).toFile();
//        if (handled.contains(file) || !filter(file))
//        {
//          continue;
//        }
//        System.out.println(kind + " " + file);
//        if (kind == StandardWatchEventKinds.ENTRY_CREATE
//            || kind == StandardWatchEventKinds.ENTRY_MODIFY)
//        {
//          deploy(file);
//        } else if (kind == StandardWatchEventKinds.ENTRY_DELETE)
//        {
//          delete(file);
//        }
//        handled.add(file);
//      }
//      key.reset();
//    }
//  }
//
//  private boolean filter(File file)
//  {
//    return !file.isDirectory();
//  }
//
//  private void deploy(File file)
//  {
//    try
//    {
//      System.out.println("Extracting " + file.getAbsolutePath());
//      File pluginFolder = extract(file);
//      System.out.println("Extracted to " + pluginFolder.getAbsolutePath());
//      File ini = new File(pluginFolder, "plugin.ini");
//      if ((!ini.exists()) || ini.isDirectory())
//      {
//        throw new IOException("plugin.ini does not exist or is not a valid file");
//      }
//      Properties props = new Properties();
//      FileInputStream iniInputStream = new FileInputStream(ini); 
//      props.load(iniInputStream);
//      iniInputStream.close();
//      String pluginNamespace = validateAndGet(props, "namespace");
//      String pluginName = validateAndGet(props, "name");
//      String pluginVersion = validateAndGet(props, "version");
//      String pluginQN = getQN(pluginNamespace, pluginName);
//      Plugin existingPlugin = plugins.get(pluginQN);
//      if (existingPlugin != null)
//      {
//        System.out.println("Deleting plugin " + pluginQN + " " + existingPlugin.getVersion());
//        for (LifecycleListener lifecycleListener : new LinkedList<LifecycleListener>(lifecycleListeners))
//        {
//          if (lifecycleListener.interests(file.getName().substring(file.getName().length() - 3)))
//          {
//            lifecycleListener.onDelete(lifecycleListeners, existingPlugin);
//          }
//        }
//        System.out.println("Deleted");
//      }
//      System.out.println("Deploying plugin " + pluginQN + " " + pluginVersion);
//      Plugin plugin = new Plugin(pluginNamespace, pluginName, pluginVersion, file.getAbsolutePath(), props);
//      plugins.put(pluginQN, plugin);
//      for (LifecycleListener lifecycleListener : new LinkedList<LifecycleListener>(lifecycleListeners))
//      {
//        if (lifecycleListener.interests(file.getName().substring(file.getName().length() - 3)))
//        {
//          lifecycleListener.onDeploy(lifecycleListeners, plugin);
//        }
//      }
//      System.out.println("Deployed");
//    } catch (IOException e)
//    {
//      System.err.println("Failed to deploy " + file.getAbsolutePath());
//      System.err.println("Exception: " + e.getClass() + " - " + e.getMessage());
//    }
//  }
//
//  private void delete(File file)
//  {
//    for (Plugin plugin : plugins.values())
//    {
//      if (plugin.getPath().equals(file.getAbsolutePath()))
//      {
//        String pluginQN = getQN(plugin.getNamespace(), plugin.getName());
//        System.out.println("Deleting plugin " + pluginQN + " " + plugin.getVersion());
//        for (LifecycleListener lifecycleListener : new LinkedList<LifecycleListener>(lifecycleListeners))
//        {
//          if (lifecycleListener.interests(file.getName().substring(file.getName().length() - 4)))
//          {
//            lifecycleListener.onDelete(lifecycleListeners, plugin);
//          }
//        }
//        System.out.println("Deleted");
//        return;
//      }
//    }
//  }
//
//  private File extract(File file) throws IOException
//  {
//    File pluginFolder = Files.createTempDirectory("deployer").toFile();
//    ZipInputStream zis = new ZipInputStream(new FileInputStream(file));
//    ZipEntry ze = zis.getNextEntry();
//    byte[] buffer = new byte[4096];
//    while (ze != null)
//    {
//      String fileName = ze.getName();
//      File newFile = new File(pluginFolder, fileName);
//      new File(newFile.getParent()).mkdirs();
//      if (ze.isDirectory())
//      {
//        new File(ze.getName()).mkdir();
//      } else
//      {
//        FileOutputStream fos = new FileOutputStream(newFile);
//        int len;
//        while ((len = zis.read(buffer)) > 0)
//        {
//          fos.write(buffer, 0, len);
//        }
//        fos.close();
//      }
//      ze = zis.getNextEntry();
//    }
//    zis.closeEntry();
//    zis.close();
//    return pluginFolder;
//  }
//
//  private String validateAndGet(Properties props, String key) throws IOException
//  {
//    String value = props.getProperty(key);
//    if (value == null)
//    {
//      throw new IOException(key + " unspecified");
//    }
//    value = value.trim();
//    if (value.length() < 1)
//    {
//      throw new IOException(key + " blank");
//    }
//    return value;
//  }
//
//  private String getQN(String pluginNamespace, String pluginName)
//  {
//    return pluginNamespace + "." + pluginName;
//  }
//
//}
