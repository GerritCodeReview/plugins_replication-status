# Replication-status

Record and display the repository's replication status without having to dig
into the Gerrit replication_log

Consumes replication events and updates a cache with the latest replication
status of specific refs to specific remotes.

The cache information is then exposed via a project's resource REST endpoint:

```bash
curl -v --user <user> '<gerrit-server>/a/projects/<project-name>/remotes/<remote-url>/replication-status'
```

* <project-name>: an (url-encoded) project repository
* <remote-url>: an (url-encoded) remote URL for the replication

For instance, to assess the replication status of the project `some/project` to
the
`https://github.com/some/project.git` URL, the following endpoint should be
called:

```bash
curl -v --user <user> '<gerrit-server>/a/projects/some%2Fproject/remotes/https%3A%2F%2Fgithub.com%2Fsome%2Fproject.git/replication-status'
```

A payload, similar to this may be returned:

```
{
  "remotes": {
    "https://github.com/some/project.git": {
      "status": {
        "refs/changes/01/1/meta": {
          "status": "SUCCEEDED",
          "when": 1626688830
        },
        "refs/changes/03/3/meta": {
          "status": "SUCCEEDED",
          "when": 1626688854
        },
        "refs/changes/03/3/1": {
          "status": "SUCCEEDED",
          "when": 1626688854
        },
        "refs/changes/02/2/1": {
          "status": "SUCCEEDED",
          "when": 1626688844
        },
        "refs/changes/02/2/meta": {
          "status": "SUCCEEDED",
          "when": 1626688844
        },
        "refs/changes/01/1/1": {
          "status": "SUCCEEDED",
          "when": 1626688830
        }
      }
    }
  },
  "status": "OK",
  "project": "some/project"
}
```

### HTTP status

The endpoint returns different HTTP response code depending on the result:

* 200 OK - All replications are successful to all remotes
* 404 Not Found - Project was not found
* 500 Failure - Not all replications are successful to all remotes
* 403 Forbidden - The user has no permission to query the endpoint. Only
  Administrators and project owners are allowed.

# TODO

* Does not consume pull-replication events.
