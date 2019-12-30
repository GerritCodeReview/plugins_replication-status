# Replication-status

Record and display the repository's replication status without having to dig
into the Gerrit replication_log

Consumes replication events and updates a cache with the latest replication
status of specific refs to specific remotes.

Since the cache is persistent, this the replication status survives through
Gerrit restarts.

The cache information is then exposed via a new project's resource REST
endpoint:

```bash
curl -v --user <user> '<gerrit-server>/a/projects/<project-name>/replication-status'
```

The output is as follows:

```
{
  "ref_status": {
    "refs/changes/21/121/meta": {
      "remote_status": {
        "/tmp/gerrit_setup/instance-2/git/push/test.git": {
          "status": "SUCCEEDED",
          "when": 1625756932
        },
        "/tmp/push/test-backup.git": {
          "status": "SUCCEEDED",
          "when": 1625756932
        },
        "/asastmp/push/test-backup.git": {
          "status": "FAILED",
          "when": 1625756932
        }
      }
    },
    "refs/changes/81/181/meta": {
      "remote_status": {
        "/tmp/gerrit_setup/instance-2/git/push/test.git": {
          "status": "SUCCEEDED",
          "when": 1625756908
        },
        "/tmp/push/test-backup.git": {
          "status": "SUCCEEDED",
          "when": 1625756908
        },
        "/asastmp/push/test-backup.git": {
          "status": "FAILED",
          "when": 1625756908
        }
      }
    },
    "refs/multi-site/version": {
      "remote_status": {
        "/tmp/gerrit_setup/instance-2/git/push/test.git": {
          "status": "SUCCEEDED",
          "when": 1625756932
        },
        "/tmp/push/test-backup.git": {
          "status": "SUCCEEDED",
          "when": 1625756932
        },
        "/asastmp/push/test-backup.git": {
          "status": "FAILED",
          "when": 1625756932
        }
      }
    },
    "refs/changes/41/141/meta": {
      "remote_status": {
        "/tmp/gerrit_setup/instance-2/git/push/test.git": {
          "status": "SUCCEEDED",
          "when": 1625756932
        },
        "/tmp/push/test-backup.git": {
          "status": "SUCCEEDED",
          "when": 1625756932
        },
        "/asastmp/push/test-backup.git": {
          "status": "FAILED",
          "when": 1625756932
        }
      }
    }
  },
  "status": "FAILED",
  "project": "push/test"
}
```

### HTTP status

The endpoint returns different HTTP response code depending on the result:

* 200 OK - All replications are successful to all remotes
* 500 Failure - Not all replications are successful to all remotes
* 403 Forbidden - The user has no permission to query the endpoint. Only
  Administrators and project owners are allowed.

# TODO

* Does not consume pull-replication events.
>>>>>>> 36b6168 (Initial implementation)
