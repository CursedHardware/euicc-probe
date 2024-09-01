package app.septs.euiccprobe

import android.util.Xml
import app.septs.euiccprobe.PrivAppPermissionParser.Companion.PrivAppPermission
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException
import java.io.InputStream
import java.nio.charset.Charset

class PrivAppPermissionParser : Iterable<PrivAppPermission> {
    companion object {
        data class PrivAppPermission(
            val packageName: String,
            val allowedPermissions: MutableSet<String>,
            val deniedPermissions: MutableSet<String>
        )
    }

    private val namespace: String? = null
    private val permissions = mutableMapOf<String, PrivAppPermission>()

    fun parse(stream: InputStream) {
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(stream, Charset.defaultCharset().toString())
        parser.nextTag()
        parser.require(XmlPullParser.START_TAG, namespace, "permissions")
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }
            when (parser.name) {
                "privapp-permissions" -> readPermission(parser)
                else -> skip(parser)
            }
        }
        parser.require(XmlPullParser.END_TAG, namespace, "permissions")
    }

    private fun readPermission(parser: XmlPullParser) {
        parser.require(XmlPullParser.START_TAG, namespace, "privapp-permissions")
        val packageName = parser.getAttributeValue(namespace, "package")
        val permission = permissions.getOrElse(packageName) {
            PrivAppPermission(packageName, mutableSetOf(), mutableSetOf())
        }
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }
            when (parser.name) {
                "permission" -> addPermission(parser, permission.allowedPermissions)
                "deny-permission" -> addPermission(parser, permission.deniedPermissions)
                else -> skip(parser)
            }
        }
        parser.require(XmlPullParser.END_TAG, namespace, "privapp-permissions")
        permissions[packageName] = permission
    }

    private fun addPermission(parser: XmlPullParser, permissions: MutableSet<String>) {
        val name = parser.name
        parser.require(XmlPullParser.START_TAG, namespace, name)
        permissions.add(parser.getAttributeValue(namespace, "name"))
        parser.nextTag()
        parser.require(XmlPullParser.END_TAG, namespace, name)
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun skip(parser: XmlPullParser) {
        if (parser.eventType != XmlPullParser.START_TAG) {
            throw IllegalStateException()
        }
        var depth = 1
        while (depth != 0) {
            when (parser.next()) {
                XmlPullParser.END_TAG -> depth--
                XmlPullParser.START_TAG -> depth++
            }
        }
    }

    override fun iterator(): Iterator<PrivAppPermission> {
        return permissions.values.iterator()
    }
}
