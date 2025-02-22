/*
 * Copyright 2021 John A. De Goes and the ZIO contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package zio.redis.benchmarks.hash

import org.openjdk.jmh.annotations._
import zio.redis._
import zio.redis.benchmarks._
import zio.{Scope => _, _}

import java.util.concurrent.TimeUnit

@State(Scope.Thread)
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
@Measurement(iterations = 15)
@Warmup(iterations = 15)
@Fork(2)
class HSetNxBenchmarks extends BenchmarkRuntime {
  @Param(Array("500"))
  var size: Int = _

  private var items: List[(String, String)] = _

  private val key = "test-hash"

  @Setup(Level.Trial)
  def setup(): Unit =
    items = (0 to size).map(e => e.toString -> e.toString).toList
  //    zioUnsafeRun(hSet(key, items.head, items.tail: _*).unit)

  @Benchmark
  def laserdisc(): Unit = {
    import _root_.laserdisc.fs2._
    import _root_.laserdisc.{all => cmd, _}
    import cats.implicits.toFoldableOps
    import cats.instances.list._
    execute[LaserDiscClient](c =>
      items.traverse_(it => c.send(cmd.hsetnx[String](Key.unsafeFrom(key), Key.unsafeFrom(it._1), it._2)))
    )
  }

  @Benchmark
  def rediculous(): Unit = {
    import cats.implicits._
    import io.chrisdavenport.rediculous._
    execute[RediculousClient](c => items.traverse_(it => RedisCommands.hsetnx[RedisIO](key, it._1, it._2).run(c)))
  }

  @Benchmark
  def redis4cats(): Unit = {
    import cats.syntax.foldable._
    execute[Redis4CatsClient[String]](c => items.traverse_(it => c.hSetNx(key, it._1, it._2)))
  }

  @Benchmark
  def zio(): Unit = execute(ZIO.foreachDiscard(items)(it => hSetNx(key, it._1, it._2)))
}
