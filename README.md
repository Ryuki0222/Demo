# Installation
build.gradle (Module: app)に以下を追加してください
	
	dependencies {
		compile 'jp.co.cyberagent.android.gpuimage:gpuimage-library:1.4.1'
		implementation 'com.github.Ryuki0222:Demo:0.1.0'
		}
	
AndroidManifest.xmlのmanifestタグ内に以下を追加してください
	
    	<uses-feature android:name="android.hardware.camera"/>
    	<uses-permission android:name="android.permission.CAMERA"/>


	