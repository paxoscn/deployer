//package cn.paxos.deployer;
//
//import java.io.File;
//import java.io.FileFilter;
//import java.io.IOException;
//import java.net.URL;
//import java.net.URLClassLoader;
//import java.util.Arrays;
//import java.util.LinkedList;
//import java.util.List;
//import java.util.Properties;
//
//public class PARListener implements LifecycleListener
//{
//
//  @Override
//  public boolean interests(String fileExtension)
//  {
//    return fileExtension.equals(".par");
//  }
//
//  @Override
//  public void onDeploy(List<LifecycleListener> lifecycleListeners, Plugin plugin) throws IOException
//  {
//    File pluginFolder = new File(plugin.getPath());
//    File classesFolder = new File(pluginFolder, "classes");
//    File libFolder = new File(pluginFolder, "lib");
//    validateFiles(classesFolder, libFolder);
//    String pluginMainBean = validateAndGet(plugin.getProps(), "main");
//    List<URL> urls = new LinkedList<URL>();
//    if (classesFolder.exists())
//    {
//      urls.add(toURL(classesFolder));
//    }
//    if (libFolder.exists())
//    {
//      for (File file : libFolder.listFiles(new FileFilter()
//      {
//        @Override
//        public boolean accept(File file)
//        {
//          return (!file.isDirectory()) && file.getName().endsWith(".jar");
//        }
//      }))
//      {
//        urls.add(toURL(file));
//      }
//    }
//    ClassLoader cl = new URLClassLoader(urls.toArray(new URL[0]), Thread.currentThread().getContextClassLoader());
//    // TODO Auto-generated method stub
//    
//  }
//  
//  private URL toURL(File f) throws IOException
//  {
//    String path = f.getCanonicalPath().replace('\\', '/');
//    if (!path.startsWith("/"))
//    {
//      path = "/" + path;
//    }
//    if (f.isDirectory() && (!path.endsWith("/")))
//    {
//      path += "/";
//    }
//    path = "file:" + path;
//    return new URL(path);
//  }
//
//  public static void main(String[] args) throws IOException
//  {
//    System.out.println(Arrays.toString(((URLClassLoader) Thread.currentThread().getContextClassLoader()).getURLs()));
//  }
//
//  @Override
//  public void onDelete(List<LifecycleListener> lifecycleListeners, Plugin plugin)
//  {
//  }
//
//  private void validateFiles(File classesFolder, File libFolder) throws IOException
//  {
//    if (classesFolder.exists() && !classesFolder.isDirectory())
//    {
//      throw new IOException("classes folder is not a valid folder");
//    }
//    if (libFolder.exists() && !libFolder.isDirectory())
//    {
//      throw new IOException("lib folder is not a valid folder");
//    }
//    if ((!classesFolder.exists()) && (!libFolder.exists()))
//    {
//      throw new IOException("either a classes folder or a lib folder must exist");
//    }
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
//}
