# Installation
build.gradle (Module: app)に以下を追加してください
	
	dependencies {
		compile 'jp.co.cyberagent.android.gpuimage:gpuimage-library:1.4.1'
		implementation 'com.github.Ryuki0222:Demo:0.1.0'
		}
	
AndroidManifest.xmlのmanifestタグ内に以下を追加してください
	
    	<uses-feature android:name="android.hardware.camera"/>
    	<uses-permission android:name="android.permission.CAMERA"/>


# Usage
layout_main.xmlなどにxmlを一行追加するだけでカメラを開くことができます。

今はまだカメラを開くだけですが、インスタグラムに写真を投稿するときのようなフィルタをかけられたり、takePicture関数だけで写真を撮れようにしようと思っています。

## filterを指定する時
	//青っぽいfilterを設定する
	mFilterCameraView.fitelr = FilterCameraView.water
	//filterを外す
	mFilterCameraView.fitelr = null
	
## 写真を撮る時
	fun mFilterCameraView_onClick (view: View) {
		val filename = "sample1.png"
		mFilterCameraView.takePicture(filename)
	}