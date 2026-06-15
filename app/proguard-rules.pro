# Audio tag parsing touches many format-specific classes; keep the package stable
# so release shrinking does not break metadata reads/writes.
-keep class org.jaudiotagger.** { *; }
-dontwarn org.jaudiotagger.**

# Room/Hilt usually provide consumer rules, but keeping local database contracts
# makes schema/migration and generated DAO access robust across release builds.
-keep class com.musicplayer.app.data.local.entity.** { *; }
-keep interface com.musicplayer.app.data.local.dao.** { *; }
