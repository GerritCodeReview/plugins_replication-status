package com.gerritforge.gerrit.plugins.replicationstatus;

import com.google.inject.Singleton;
import com.google.common.annotations.VisibleForTesting;
import java.util.*;

/**
 * This class stores the replication status of a Gerrit instance
 *
 * <p>The status is represented per project but also globally. The global replication status is, for example,
 * the max replication timestamp of all the projects.
 * The replication Status of a project is represented by
 * {@see com.gerritforge.gerrit.plugins.replicationstatus.ProjectReplicationStatus}
 */

@Singleton
public class StatusStore {

  private Map<String, ProjectReplicationStatus> statusPerProject;
  private Long globalLastReplicationTime;

  public StatusStore() {
    this.statusPerProject = new HashMap<String, ProjectReplicationStatus>();
  }

  public void updateLastReplicationTime(String projectName, Long timestamp) {
    ProjectReplicationStatus projectReplicationStatus = new ProjectReplicationStatus(timestamp);
    this.statusPerProject.put(projectName, projectReplicationStatus);
    this.globalLastReplicationTime = timestamp;
  }

  public Optional<Long> getLastReplicationTime(String projectName) {
    Optional<ProjectReplicationStatus> maybeProjectReplicationStatus = Optional.ofNullable(this.statusPerProject.get(projectName));
    if (maybeProjectReplicationStatus.isPresent()) {
      // XXX Possibly to check for existence if lastReplicationTimeStamp
      return Optional.ofNullable(maybeProjectReplicationStatus.get().getLastReplicationTimestamp());
    }
    else {
      return Optional.empty();
    }
  }

  public Long getGlobalLastReplicationTime() {
    if (this.globalLastReplicationTime != null) {
      return this.globalLastReplicationTime;
    }
    else {
      return 0L;
    }
  }


  @VisibleForTesting
  public void emptyReplicationTimePerProject() {
    this.statusPerProject.clear();
  }

}
