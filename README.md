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

## 参考文献

1. [CS61B-Project3-Gitlet](https://inst.eecs.berkeley.edu/~cs61b/fa19/materials/proj/proj3/index.html)
2. [Pro Git 2nd](https://git-scm.com/book/en/v2)
3. [git 的合并原理（递归三路合并算法）](https://blog.walterlv.com/post/git-merge-principle.html)