package com.gerritforge.gerrit.plugins.replicationstatus;

import static com.google.common.truth.Truth.assertThat;
import org.junit.Test;
import java.time.Instant;

public class StatusStoreTest {


  @Test
  public void shouldUpdateGlobalStatus() {

    Instant instant = Instant.now();
    Long nowish = instant.toEpochMilli();
    StatusStore statusStore = new StatusStore();
    statusStore.updateLastReplicationTime("myProject", nowish);

    assertThat(statusStore.getGlobalLastReplicationTime()).isEqualTo(nowish);
  }

}
