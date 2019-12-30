# Replication-status

Record and display the repository's replication status without having to dig
into the Gerrit replication_log

Consumes replication events and updates a cache with the latest replication
status of specific refs to specific remotes.

The cache information is then exposed via a new project's resource REST
endpoint:

```bash
curl -v --user <user> '<gerrit-server>/a/projects/<project-name>/remotes/<remote-name>/replication-status'
```

* <project-name>: is an (url-encoded) project repository
* <remote-name>: is an (url-encoded) name representing a "remote" stanza in
  the `replication.config`

Note, since multiple URLs may be specified within a single `remote` block,
multiple remotes may be returned in the payload.

For instance, given the following `remote` stanza in the `replication.config`:

```
[remote "some-remote"]
    url = https://github.com/${name}.git
    url = ssh://some.mirror.com/${name}.git
    push = +refs/*:refs/*
    projects = some/project
```

and the endpoint is called as such:

```bash
curl -v --user <user> '<gerrit-server>/a/projects/some%2Fproject/remotes/some%2Fremote/replication-status'
```

A payload, similar to this may be returned:

```
{
  "remotes": {
    "https://github.com/some/project.git": {
      "status": {
        "refs/changes/22/22/meta": {
          "status": "SUCCEEDED",
          "when": 1626372616
        },
        "refs/changes/21/21/meta": {
          "status": "SUCCEEDED",
          "when": 1626372294
        }
      }
    },
    "ssh://some.mirror.com/some/project.git": {
      "status": {
        "refs/changes/22/22/meta": {
          "status": "SUCCEEDED",
          "when": 1626372616
        },
        "refs/changes/21/21/meta": {
          "status": "SUCCEEDED",
          "when": 1626372294
        }
      }
    }
  },
  "status": "OK",
  "project": "push/test"
}
```

### HTTP status

The endpoint returns different HTTP response code depending on the result:

* 200 OK - All replications are successful to all remotes
* 404 Not Found - Project or remote for project was not found
* 500 Failure - Not all replications are successful to all remotes
* 403 Forbidden - The user has no permission to query the endpoint. Only
  Administrators and project owners are allowed.

# TODO

* Does not consume pull-replication events.
