package com.example.opengl

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import com.example.opengl.R
//import io.flutter.embedding.android.FlutterFragment
//import io.flutter.embedding.engine.FlutterEngine
//import io.flutter.embedding.engine.FlutterEngineCache
//import io.flutter.embedding.engine.dart.DartExecutor


class FlutterFragmentActivity : FragmentActivity() {
//    private var flutterFragment : FlutterFragment? = null
    private val TAG_FLUTTER_FRAGMENT = "flutter_fragment"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_flutter_fragment)
        val fragmentManager: FragmentManager = supportFragmentManager
//        var flutterEngine = FlutterEngine(this)
//        flutterEngine.getDartExecutor().executeDartEntrypoint(
//            DartExecutor.DartEntrypoint.createDefault()
//        )
//        FlutterEngineCache
//            .getInstance()
//            .put("my_engine", flutterEngine);
//        flutterFragment = fragmentManager.findFragmentByTag(TAG_FLUTTER_FRAGMENT) as FlutterFragment?
//        if (flutterFragment == null) {
//            var newFlutterFragment = FlutterFragment.createDefault()
//            var newFlutterFragment = FlutterFragment.withCachedEngine("my_engine").build()
//            flutterFragment = newFlutterFragment
//            fragmentManager
//                .beginTransaction()
//                .add(
//                    R.id.fragment_container,
//                    newFlutterFragment,
//                    TAG_FLUTTER_FRAGMENT
//                )
//                .commit();
//        }
    }

    override fun onPostResume() {
        super.onPostResume()
//        flutterFragment?.onPostResume()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
//        flutterFragment?.onNewIntent(intent!!)
    }

    override fun onBackPressed() {
        super.onBackPressed()
//        flutterFragment?.onBackPressed()
    }
}