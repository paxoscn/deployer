package cn.paxos.deployer.example.dummy;

import cn.paxos.deployer.PluginContext;

public class Main
{
  
  public static void main(String[] args)
  {
    PluginContext.attributes.put("dummy", new DummyImpl());
  }

}
