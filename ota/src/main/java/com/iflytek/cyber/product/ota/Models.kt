package com.iflytek.cyber.product.ota

import java.util.*

data class ReportEntity(val pids: List<Long>) {
    companion object {
        fun from(manifests: List<ManifestEntity>): ReportEntity {
            val pids = ArrayList<Long>()
            manifests.map {
                pids.add(it.id)
            }
            return ReportEntity(pids)
        }

        fun from(manifest: ManifestEntity): ReportEntity {
            return ReportEntity(listOf(manifest.id))
        }
    }
}

data class PackageEntity(val id: Long, val revision: Long, val identity: String, val url: String)

data class ManifestEntity(val id: Long, val revision: Long, val identity: String) {
    companion object {
        fun from(pkg: PackageEntity): ManifestEntity {
            return ManifestEntity(pkg.id, pkg.revision, pkg.identity)
        }
    }
}

data class VersionCodeAndId(val versionCode: Int, val pid: Long)

data class VersionCodeMap(val set: Set<VersionCodeAndId>)