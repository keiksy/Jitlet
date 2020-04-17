# Jitlet：一个简易的版本控制系统

Jitlet即Gitlet的谐音，意为一个小型的Git，首字母J意在表明本项目由Java编写。

Jitlet支持了绝大多数的版本控制功能。

## 功能

1. 在当前目录初始化gitlet仓库：

```
java Gitlet init
```

2. 跟踪或者暂存最新版文件

```
java Gitlet add [filename]
```

3. 添加分支

```
java Gitlet branch [branch_name]
```

4. 检出到指定分支

```
java Gitlet checkout [branch_name]
```

5. 提交暂存区

```
java Gitlet commit [log_text]
```

6. 打印出所有log为给定log的全部提交记录

```
java Gitlet find [log_text]
```

7. 打印出本gitlet仓库的所有提交记录

```
java Gitlet global-log
```

8. 按照时间逆序打印当前分支的所有历史提交记录，直到第一次提交

```
java Gitlet log
```

9. 合并当前分支和指定分支。

```
java Gitlet merge [branch_name]
```

10. 检出到指定提交

```
java Gitlet reset [commit_id]
```

11. 将指定文件从暂存区删除，同时也在磁盘上删除该文件

```
java Gitlet rm [filename]
```

12. 删除指定分支

```
java Gitlet rm-branch [branch_name]
```

13. 打印当前状态

```
java Gitlet status
```
功能和`git status`一致。

14. 支持对文件夹进行版本控制操作

## 待开发功能

~~1. 子文件夹支持。~~(2020/04/16填坑)

2. 远程仓库相关功能。

## 内部原理
- Gitapp是主类，包含三大组件的单例：暂存区（Stage.java），提交树（CommitChain.java）和文件抽象池（BlobPool.java）
- 暂存区就是stage，数据结构是将文件名（相对于git仓库主文件夹的相对路径）映射到文件的hash的Map
- 提交树保存本Repo所有的提交，数据结构是将commitStr（一次Commit的hash字符串的前六位）映射为Commit对象的Map
- 提交树还保存了本Repo所有的分支，数据结构是将分支名映射为commitStr的Map
- 文件抽象池是为了实现特定文件名的特定版本只占用一次磁盘空间的优化，同时将磁盘IO操作和其他负责业务逻辑的类解耦，数据结构是
将文件的hash映射为Blob对象的Map
- Blob对象是对一个文件的抽象，跟踪了一个文件的磁盘位置和git文件夹内快照的位置

## 参考文献

1. [CS61B-Project3-Gitlet](https://inst.eecs.berkeley.edu/~cs61b/fa19/materials/proj/proj3/index.html)
2. [Pro Git 2nd](https://git-scm.com/book/en/v2)
3. [git 的合并原理（递归三路合并算法）](https://blog.walterlv.com/post/git-merge-principle.html)