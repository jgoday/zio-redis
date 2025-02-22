package zio.redis

import zio.Chunk
import zio.test.Assertion._
import zio.test._

trait HashSpec extends BaseSpec {
  def hashSuite: Spec[TestConfig with Redis, RedisError] =
    suite("hash")(
      suite("hSet, hGet, hGetAll and hDel")(
        test("set followed by get") {
          for {
            hash   <- uuid
            field  <- uuid
            value  <- uuid
            _      <- hSet(hash, field -> value)
            result <- hGet(hash, field).returning[String]
          } yield assert(result)(isSome(equalTo(value)))
        },
        test("set multiple fields for hash") {
          for {
            hash   <- uuid
            field1 <- uuid
            field2 <- uuid
            value  <- uuid
            result <- hSet(hash, field1 -> value, field2 -> value)
          } yield assert(result)(equalTo(2L))
        },
        test("get all fields for hash") {
          for {
            hash   <- uuid
            field1 <- uuid
            field2 <- uuid
            value  <- uuid
            _      <- hSet(hash, field1 -> value, field2 -> value)
            result <- hGetAll(hash).returning[String, String]
          } yield assert(Chunk.fromIterable(result.values))(hasSameElements(Chunk(value, value)))
        },
        test("delete field for hash") {
          for {
            hash    <- uuid
            field   <- uuid
            value   <- uuid
            _       <- hSet(hash, field -> value)
            deleted <- hDel(hash, field)
            result  <- hGet(hash, field).returning[String]
          } yield assert(deleted)(equalTo(1L)) && assert(result)(isNone)
        },
        test("delete multiple fields for hash") {
          for {
            hash    <- uuid
            field1  <- uuid
            field2  <- uuid
            value   <- uuid
            _       <- hSet(hash, field1 -> value, field2 -> value)
            deleted <- hDel(hash, field1, field2)
          } yield assert(deleted)(equalTo(2L))
        }
      ),
      suite("hmSet and hmGet")(
        test("set followed by get") {
          for {
            hash   <- uuid
            field  <- uuid
            value  <- uuid
            _      <- hmSet(hash, field -> value)
            result <- hmGet(hash, field).returning[String]
          } yield assert(result)(hasSameElements(Chunk(Some(value))))
        },
        test("set multiple fields for hash") {
          for {
            hash    <- uuid
            field1  <- uuid
            field2  <- uuid
            value   <- uuid
            _       <- hmSet(hash, field1 -> value, field2 -> value)
            result1 <- hmGet(hash, field1).returning[String]
            result2 <- hmGet(hash, field2).returning[String]
          } yield assert(result1)(hasSameElements(Chunk(Some(value)))) &&
            assert(result2)(hasSameElements(Chunk(Some(value))))
        },
        test("get multiple fields for hash") {
          for {
            hash   <- uuid
            field1 <- uuid
            field2 <- uuid
            value1 <- uuid
            value2 <- uuid
            _      <- hmSet(hash, field1 -> value1, field2 -> value2)
            result <- hmGet(hash, field1, field2).returning[String]
          } yield assert(result)(hasSameElements(Chunk(Some(value1), Some(value2))))
        },
        test("delete field for hash") {
          for {
            hash    <- uuid
            field   <- uuid
            value   <- uuid
            _       <- hmSet(hash, field -> value)
            deleted <- hDel(hash, field)
            result  <- hmGet(hash, field).returning[String]
          } yield assert(deleted)(equalTo(1L)) && assert(result)(hasSameElements(Chunk(None)))
        },
        test("delete multiple fields for hash") {
          for {
            hash    <- uuid
            field1  <- uuid
            field2  <- uuid
            field3  <- uuid
            value   <- uuid
            _       <- hmSet(hash, field1 -> value, field2 -> value, field3 -> value)
            deleted <- hDel(hash, field1, field3)
            result  <- hmGet(hash, field1, field2, field3).returning[String]
          } yield assert(deleted)(equalTo(2L)) &&
            assert(result)(hasSameElements(Chunk(None, Some(value), None)))
        }
      ),
      suite("hExists")(
        test("field should exist") {
          for {
            hash   <- uuid
            field  <- uuid
            value  <- uuid
            _      <- hSet(hash, field -> value)
            result <- hExists(hash, field)
          } yield assert(result)(isTrue)
        },
        test("field shouldn't exist") {
          for {
            hash   <- uuid
            field  <- uuid
            result <- hExists(hash, field)
          } yield assert(result)(isFalse)
        }
      ),
      suite("hIncrBy and hIncrByFloat")(
        test("existing field should be incremented by 1") {
          for {
            hash   <- uuid
            field  <- uuid
            _      <- hSet(hash, field -> "1")
            result <- hIncrBy(hash, field, 1L)
          } yield assert(result)(equalTo(2L))
        },
        test("incrementing value of non-existing hash and filed should create them") {
          for {
            hash   <- uuid
            field  <- uuid
            result <- hIncrBy(hash, field, 1L)
          } yield assert(result)(equalTo(1L))
        },
        test("existing field should be incremented by 1.5") {
          for {
            hash   <- uuid
            field  <- uuid
            _      <- hSet(hash, field -> "1")
            result <- hIncrByFloat(hash, field, 1.5)
          } yield assert(result)(equalTo(2.5))
        },
        test("incrementing value of float for non-existing hash and field should create them") {
          for {
            hash   <- uuid
            field  <- uuid
            result <- hIncrByFloat(hash, field, 1.5)
          } yield assert(result)(equalTo(1.5))
        },
        test("incrementing value of float for non-existing hash and field with negative value") {
          for {
            hash   <- uuid
            field  <- uuid
            result <- hIncrByFloat(hash, field, -1.5)
          } yield assert(result)(equalTo(-1.5))
        }
      ),
      suite("hKeys and hLen")(
        test("get field names for existing hash") {
          for {
            hash   <- uuid
            field  <- uuid
            value  <- uuid
            _      <- hSet(hash, field -> value)
            result <- hKeys(hash).returning[String]
          } yield assert(result)(hasSameElements(Chunk(field)))
        },
        test("get field names for non-existing hash") {
          for {
            hash   <- uuid
            result <- hKeys(hash).returning[String]
          } yield assert(result)(isEmpty)
        },
        test("get field count for existing hash") {
          for {
            hash   <- uuid
            field  <- uuid
            value  <- uuid
            _      <- hSet(hash, field -> value)
            result <- hLen(hash)
          } yield assert(result)(equalTo(1L))
        },
        test("get field count for non-existing hash") {
          for {
            hash   <- uuid
            result <- hLen(hash)
          } yield assert(result)(equalTo(0L))
        }
      ),
      suite("hSetNx")(
        test("set value for non-existing field") {
          for {
            hash   <- uuid
            field  <- uuid
            value  <- uuid
            result <- hSetNx(hash, field, value)
          } yield assert(result)(isTrue)
        },
        test("set value for existing field") {
          for {
            hash   <- uuid
            field  <- uuid
            value  <- uuid
            _      <- hSet(hash, field -> value)
            result <- hSetNx(hash, field, value)
          } yield assert(result)(isFalse)
        }
      ),
      suite("hStrLen")(
        test("get value length for existing field") {
          for {
            hash   <- uuid
            field  <- uuid
            value  <- uuid
            _      <- hSet(hash, field -> value)
            result <- hStrLen(hash, field)
          } yield assert(result)(equalTo(value.length.toLong))
        },
        test("get value length for non-existing field") {
          for {
            hash   <- uuid
            field  <- uuid
            result <- hStrLen(hash, field)
          } yield assert(result)(equalTo(0L))
        }
      ),
      suite("hVals")(
        test("get all values from existing hash") {
          for {
            hash   <- uuid
            field  <- uuid
            value  <- uuid
            _      <- hSet(hash, field -> value)
            result <- hVals(hash).returning[String]
          } yield assert(result)(hasSameElements(Chunk(value)))
        },
        test("get all values from non-existing hash") {
          for {
            hash   <- uuid
            result <- hVals(hash).returning[String]
          } yield assert(result)(isEmpty)
        }
      ),
      suite("hScan")(
        test("hScan entries with match and count options")(check(genPatternOption, genCountOption) { (pattern, count) =>
          for {
            hash            <- uuid
            field           <- uuid
            value           <- uuid
            _               <- hSet(hash, field -> value)
            scan            <- hScan(hash, 0L, pattern, count).returning[String, String]
            (next, elements) = scan
          } yield assert(next)(isGreaterThanEqualTo(0L)) && assert(elements)(isNonEmpty)
        })
      ),
      suite("hRandField")(
        test("randomly select one field") {
          for {
            hash   <- uuid
            field1 <- uuid
            value1 <- uuid
            field2 <- uuid
            value2 <- uuid
            _      <- hSet(hash, field1 -> value1, field2 -> value2)
            field  <- hRandField(hash).returning[String]
          } yield assert(Seq(field1, field2))(contains(field.get))
        },
        test("returns None if key does not exists") {
          for {
            hash    <- uuid
            field   <- uuid
            value   <- uuid
            badHash <- uuid
            _       <- hSet(hash, field -> value)
            field   <- hRandField(badHash).returning[String]
          } yield assert(field)(isNone)
        },
        test("returns n different fields if count is provided") {
          for {
            hash   <- uuid
            field1 <- uuid
            value1 <- uuid
            field2 <- uuid
            value2 <- uuid
            _      <- hSet(hash, field1 -> value1, field2 -> value2)
            fields <- hRandField(hash, 2).returning[String]
          } yield assert(fields)(hasSize(equalTo(2)))
        },
        test("returns all hash fields if count is provided and is greater or equal than hash size") {
          for {
            hash   <- uuid
            field1 <- uuid
            value1 <- uuid
            field2 <- uuid
            value2 <- uuid
            _      <- hSet(hash, field1 -> value1, field2 -> value2)
            fields <- hRandField(hash, 4).returning[String]
          } yield assert(fields)(hasSize(equalTo(2)))
        },
        test("returns repeated fields if count is negative") {
          for {
            hash   <- uuid
            field  <- uuid
            value  <- uuid
            _      <- hSet(hash, field -> value)
            fields <- hRandField(hash, -2).returning[String]
          } yield assert(fields)(hasSameElements(Chunk(field, field)))
        },
        test("returns n different fields and values with 'withvalues' option") {
          for {
            hash   <- uuid
            field1 <- uuid
            value1 <- uuid
            field2 <- uuid
            value2 <- uuid
            _      <- hSet(hash, field1 -> value1, field2 -> value2)
            fields <- hRandField(hash, 4, withValues = true).returning[String]
          } yield assert(fields)(hasSize(equalTo(4)))
        }
      )
    )
}
