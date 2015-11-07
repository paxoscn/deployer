package cn.paxos.deployer;

import java.util.Properties;

public class Plugin
{

  private final String namespace;
  private final String name;
  private final String version;
  private final String path;
  private final ClassLoader classLoader;
  private final Properties props;
  
  public Plugin(String namespace, String name, String version, String path,
      ClassLoader classLoader, Properties props)
  {
    this.namespace = namespace;
    this.name = name;
    this.version = version;
    this.path = path;
    this.classLoader = classLoader;
    this.props = props;
  }
  
  public String getNamespace()
  {
    return namespace;
  }
  public String getName()
  {
    return name;
  }
  public String getVersion()
  {
    return version;
  }
  public String getPath()
  {
    return path;
  }
  public ClassLoader getClassLoader()
  {
    return classLoader;
  }
  public Properties getProps()
  {
    return props;
  }

}
