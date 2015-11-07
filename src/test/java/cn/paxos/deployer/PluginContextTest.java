package cn.paxos.deployer;

import java.io.IOException;

import org.junit.Test;

import cn.paxos.deployer.example.dummy.listener.DummyListener;

public class PluginContextTest
{

  @Test
  public void testStart() throws IOException, InterruptedException
  {
    new PluginContext("/var/plugins", new DummyListener());
  }

}
