### Publish the Android Library onto GitHub Package Registry

- Make sure to build and run the tasks to generate the library files inside ***build/outputs/aar/*** before proceeding to publish the library.

- Execute the **Publish** gradle task which is inside your library module

  ```bash
  $ ./gradlew PjDroid:assemble
  $ ./gradlew PjDroid:publish
  ```

- Once the task is successful you should be able to see the Package under the **Packages** tab of the GitHub Account

- In case of a failure run the task with *--stacktrace*, *--info* or *--debug* to check the logs for detailed information about the causes.

# See

- https://github.com/enefce/AndroidLibraryForGitHubPackagesDemo
- https://dev.to/mohitrajput987/publish-android-library-using-github-packages-4lnf
- https://www.droidcon.com/news-detail?content-id=/repository/collaboration/Groups/spaces/droidcon_hq/Documents/public/news/android-news/Using%20Kotlin%20DSL%20to%20publish%20an%20Android%20library%20to%20GitHub%20Packages