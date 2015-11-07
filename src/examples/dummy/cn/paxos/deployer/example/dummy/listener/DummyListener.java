package cn.paxos.deployer.example.dummy.listener;

import cn.paxos.deployer.LifecycleListener;
import cn.paxos.deployer.Plugin;
import cn.paxos.deployer.PluginContext;
import cn.paxos.deployer.example.dummy.Dummy;

public class DummyListener implements LifecycleListener
{

  @Override
  public void onDeploy(Plugin plugin)
  {
    Dummy dummy = (Dummy) PluginContext.attributes.get("dummy");
    dummy.go();
  }

  @Override
  public void onDelete(Plugin plugin)
  {
  }

}
