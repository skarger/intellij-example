# IntelliJ Bug Example

### Environment

IntelliJ IDEA 2018.3.5 (Ultimate Edition)
Build #IU-183.5912.21, built on February 26, 2019
JRE: 1.8.0_152-release-1343-b28 x86_64
JVM: OpenJDK 64-Bit Server VM by JetBrains s.r.o
macOS 10.13.6

I have the JetBrains Ruby plugin installed.

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

### The problem

We use Jenkins build servers to deploy several Rails apps. Jenkins build pipelines are defined with Groovy. So within the Rails repositories, we have files such as deployment/jenkins/my-app-name-deploy.groovy.

When I try to import any of these repositories into IntelliJ (File -> New -> Project from Existing Sources...), the only source files it detects are the .groovy files. So it thinks the project root is deployment/jenkins/. It does not detect the Ruby files.

Then if I temporarily remove the Groovy files from the project (rm deployment/*/*.groovy), I can import it to IntelliJ without problems. Then it will correctly use the top-level directory I chose as the project root, and detect that it is a Ruby project. Finally I can restore my *.groovy files, and from then on the project works correctly in IntelliJ.

It would be nice if it could correctly understand that it's a Ruby project without removing the files. Or allow me to set the root and project type explicitly when creating from existing sources.
