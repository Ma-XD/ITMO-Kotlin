# Задание 4. Coroutines. Базовые возможности

Задание состоит из нескольких модулей внутри директории `tasks`, каждый из которых содержит в себе `Readme.md` описание задания, заготовку решения в `src` и набор unit-тестов для проверки решения.
Ваша задача - решить задания в каждому модуле.  

## Требования к решению
Задания предполагают использования библиотеки `kotlinx.coroutines`.

При выполнении задания не допускается использования Java Concurrency Utilities, в частности пакета `java.util.concurrent`.
Так же не стоит использовать библиотеку `kotlinx.atomicfu`.

## Задания

- [Application](tasks/1-application#readme)
- [Sequential processor](tasks/2-sequential%20processor#readme) (Техн. `Coroutine Context`)
- [Parallel Evaluator](tasks/3-parallel-evaluator#readme) (Техн. `Coroutine Scope`)
- [CompletableFuture](tasks/4-feature#readme) (Техн. `Cancellable Coroutine`)
- [Once](tasks/5-once#readme) (Техн. `Mutex`)
- [Image processor](tasks/6-image-processor#readme) (Техн. `Channels`)
- [Competition results](tasks/7-competition-results#readme) (Техн. `Flow`)

Все решения должны быть эффективными по времени и памяти.
Решение можно проверить с помощью тестов, которые запускаются в интерфейсе Intellij Idea или через консоль `./gradlew test` (*Nix) или `.\gradlew test` (Windows).


