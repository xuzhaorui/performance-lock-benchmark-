# performance-lock-benchmark-
比 synchronized 快 30%，我从零构建了一把极致性能的 Java 锁 在并发编程中，锁是性能的天花板。一次错误的选择，可能让你的系统永远跑不到理想状态。于是我从零出发，构建了几种高性能锁，并用 JMH 逐个实测，目标只有一个：让它比 synchronized 至少快 30%。
