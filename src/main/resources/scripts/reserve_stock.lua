local stockKey = KEYS[1]
local purchasedUsersSetKey = KEYS[2]
local syncQueueListKey = KEYS[3]

local userId = ARGV[1]
local requestedQuantity = tonumber(ARGV[2])

-- 1. Check if item exists and get current stock
local currentStock = redis.call('GET', stockKey)
if not currentStock then 
    return -1 -- Item not found
end

-- 2. Check if stock is sufficient
if tonumber(currentStock) >= requestedQuantity then
    
    -- 3. Check if user already bought (using the SET)
    local alreadyBought = redis.call('SISMEMBER', purchasedUsersSetKey, userId)
    if alreadyBought == 1 then 
        return -2 -- User already reserved
    end

    -- 4. Proceed with reservation
    redis.call('DECRBY', stockKey, requestedQuantity)
    
    -- 5. Add user to the permanent SET to prevent future duplicates
    redis.call('SADD', purchasedUsersSetKey, userId)
    
    -- 6. Push to the LIST queue for the background worker to pick up
    local queueValue = userId .. ':' .. requestedQuantity
    redis.call('RPUSH', syncQueueListKey, queueValue)

    return 1 -- Success
else
    return 0 -- Sold out
end