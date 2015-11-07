package cn.paxos.deployer;

import java.io.IOException;
import java.util.List;

public interface CopyOfLifecycleListener
{

  boolean interests(String fileExtension);

  void onDeploy(List<CopyOfLifecycleListener> lifecycleListeners, Plugin plugin) throws IOException;

  void onDelete(List<CopyOfLifecycleListener> lifecycleListeners, Plugin plugin);

}
