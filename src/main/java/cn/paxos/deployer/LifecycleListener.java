package cn.paxos.deployer;

public interface LifecycleListener
{

  void onDeploy(Plugin plugin);

  void onDelete(Plugin plugin);

}
