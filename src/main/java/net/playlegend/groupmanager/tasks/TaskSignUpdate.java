package net.playlegend.groupmanager.tasks;

import net.playlegend.groupmanager.GroupManagerPlugin;

import java.util.logging.Level;

public class TaskSignUpdate implements Runnable {

  @Override
  public void run() {
    try {
      GroupManagerPlugin.getInstance().getSignManager().updateSigns();
    } catch (Exception e) {
      GroupManagerPlugin.getInstance().log(Level.WARNING, "Failed to update signs.", e);
    }
  }
}
