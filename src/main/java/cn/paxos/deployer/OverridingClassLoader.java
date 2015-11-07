package cn.paxos.deployer;

import java.net.URL;
import java.net.URLClassLoader;

public class OverridingClassLoader extends URLClassLoader
{
  
  private final ClassLoader parent;

  public OverridingClassLoader(URL[] urls, ClassLoader parent)
  {
    super(urls, null);
    this.parent = parent;
  }

  @Override
  public Class<?> loadClass(String name) throws ClassNotFoundException
  {
    try
    {
      return super.loadClass(name);
    } catch (Exception e)
    {
      return parent.loadClass(name);
    }
  }

}
