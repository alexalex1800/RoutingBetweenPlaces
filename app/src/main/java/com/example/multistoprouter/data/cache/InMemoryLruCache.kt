package com.example.multistoprouter.data.cache

class InMemoryLruCache<K, V>(private val maxSize: Int) {
    init {
        require(maxSize > 0) { "maxSize must be > 0" }
    }

    private val map = object : LinkedHashMap<K, V>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, V>?): Boolean {
            return size > maxSize
        }
    }

    @Synchronized
    fun get(key: K): V? = map[key]

    @Synchronized
    fun put(key: K, value: V) {
        map[key] = value
    }

    @Synchronized
    fun clear() {
        map.clear()
    }
}
