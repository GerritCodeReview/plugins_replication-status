package com.gerritforge.gerrit.plugins.replicationstatus;

/**
 * This class contains the replication status of a Gerrit project
 *
 * <p>NOTE: Currently, the status is only represented by the last replication timestamp, but it could be extended (i.e.:
 * the last replicated commit can be an interesting information to add.
 */
public class ProjectReplicationStatus {

  private Long lastReplicationTimestamp;

  public ProjectReplicationStatus(Long lastReplicationTimestamp) {
    this.lastReplicationTimestamp = lastReplicationTimestamp;
  }

  public Long getLastReplicationTimestamp() {
	  return this.lastReplicationTimestamp;
  }

  public Long setLastReplicationTimestamp(Long lastReplicationTimestamp) {
	  return this.lastReplicationTimestamp = lastReplicationTimestamp;
  }

}
