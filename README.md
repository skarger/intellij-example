# IntelliJ Bug Example

Ruby and Rails versions should not be important to the issue, but for reference:
```
$ ruby --version
ruby 2.6.1p33 (2019-01-30 revision 66950) [x86_64-darwin17]
$ rails --version
Rails 5.2.2
```

On my local computer I am using [rbenv](https://github.com/rbenv/rbenv) to manage Ruby versions.


I generated this repository like so:

```
$ rails new intellij-example
```

In real apps we would pass different options to `rails new` (such as to use Postgres DB), but that does not seem to matter. This example reproduces the problem, at least with my IntelliJ install.
