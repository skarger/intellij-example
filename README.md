# IntelliJ Bug Example

Ruby and Rails version should not be important to the issue, but for reference:
```
$ ruby --version
ruby 2.6.1p33 (2019-01-30 revision 66950) [x86_64-darwin17]
$ rails --version
Rails 5.2.2
```

I am using [rbenv](https://github.com/rbenv/rbenv) to manage Ruby versions.


I generated this repository like so:

```
$ rails new intellij-example
```

In the real apps we would have passed the Postgres DB option to `rails new` as well as other config, but I believe those details are unimportant.
