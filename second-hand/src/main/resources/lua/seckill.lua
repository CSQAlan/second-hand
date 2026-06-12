local stockKey = KEYS[1]
local userKey = KEYS[2]
local userId = ARGV[1]

-- 1. 判断用户是否已抢购过
if redis.call("sismember", userKey, userId) == 1 then
    return -1 -- 代表重复抢购
end

-- 2. 判断库存是否充足
local stock = tonumber(redis.call("get", stockKey))
if not stock or stock <= 0 then
    return -2 -- 代表库存不足（已售罄）
end

-- 3. 扣减库存并记录用户
redis.call("decr", stockKey)
redis.call("sadd", userKey, userId)
return 1 -- 成功抢购
