package net.playlegend.groupmanager.tasks;

import net.playlegend.groupmanager.GroupManager;

import java.util.logging.Level;

public class TaskSignUpdate implements Runnable {

  @Override
  public void run() {
    try {
      GroupManager.getInstance().getSignManager().updateSigns();
    } catch (Exception e) {
      GroupManager.getInstance().log(Level.WARNING, "Failed to update signs.", e);
    }
  }
}
