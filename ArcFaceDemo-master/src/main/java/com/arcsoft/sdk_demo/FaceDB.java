package com.arcsoft.sdk_demo;

import android.util.Log;

import com.arcsoft.facerecognition.AFR_FSDKEngine;
import com.arcsoft.facerecognition.AFR_FSDKError;
import com.arcsoft.facerecognition.AFR_FSDKFace;
import com.arcsoft.facerecognition.AFR_FSDKVersion;
import com.guo.android_extend.java.ExtInputStream;
import com.guo.android_extend.java.ExtOutputStream;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by gqj3375 on 2017/7/11.
 */

public class FaceDB {
	private final String TAG = this.getClass().toString();

	public static String appid = "C6zLjbFy99Px96oS1S8PnZNCmxTwuDnZeuynbqLt8NzN";
	public static String ft_key = "8JqkZ4wPQrrpQabCW3j9XR22EJyhuXMux6s2J5Mi8Zkf";
	public static String fd_key = "8JqkZ4wPQrrpQabCW3j9XR29PiEpcbPVPnu3zMWUdZyU";
	public static String fr_key = "8JqkZ4wPQrrpQabCW3j9XR2Wsv2LBSEwfac1qbSiy3aa";
	public static String age_key = "8JqkZ4wPQrrpQabCW3j9XR2tN7orFYz4fxgZocQrGQGv";
	public static String gender_key = "8JqkZ4wPQrrpQabCW3j9XR31XX52LwgacSLqaL3ZAJmN";

	String mDBPath;
	List<FaceRegist> mRegister;
	AFR_FSDKEngine mFREngine;
	AFR_FSDKVersion mFRVersion;
	boolean mUpgrade;

	class FaceRegist {
		//人名
		String mName;
		//同一个人的多个特征
		List<AFR_FSDKFace> mFaceList;

		public FaceRegist(String name) {
			mName = name;
			mFaceList = new ArrayList<>();
		}
	}

	public FaceDB(String path) {
		mDBPath = path;
		mRegister = new ArrayList<>();
		mFRVersion = new AFR_FSDKVersion();
		mUpgrade = false;
		mFREngine = new AFR_FSDKEngine();
		AFR_FSDKError error = mFREngine.AFR_FSDK_InitialEngine(FaceDB.appid, FaceDB.fr_key);
		if (error.getCode() != AFR_FSDKError.MOK) {
			Log.e(TAG, "AFR_FSDK_InitialEngine fail! error code :" + error.getCode());
		} else {
			mFREngine.AFR_FSDK_GetVersion(mFRVersion);
			Log.d(TAG, "AFR_FSDK_GetVersion=" + mFRVersion.toString());
		}
	}

	public void destroy() {
		if (mFREngine != null) {
			mFREngine.AFR_FSDK_UninitialEngine();
		}
	}

	private boolean saveInfo() {
		try {
			FileOutputStream fs = new FileOutputStream(mDBPath + "/face.txt");
			ExtOutputStream bos = new ExtOutputStream(fs);
			bos.writeString(mFRVersion.toString() + "," + mFRVersion.getFeatureLevel());
			bos.close();
			fs.close();
			return true;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}

	private boolean loadInfo() {
	    //不为空说明已经加载过数据了
		if (!mRegister.isEmpty()) {
			return false;
		}
		try {
			FileInputStream fs = new FileInputStream(mDBPath + "/face.txt");
			ExtInputStream bos = new ExtInputStream(fs);
			//load version
			String version_saved = bos.readString();
            Log.d(TAG, "loadInfo: "+ version_saved);
            //当前使用的sdk版本
			if (version_saved.equals(mFRVersion.toString() + "," + mFRVersion.getFeatureLevel())) {
			    //版本信息与保存一致
				mUpgrade = true;
			}
			//load all regist name.
			if (version_saved != null) {
			    //不断读取姓名，加载人脸数据到内存 mRegister 是sdk中保存的全部人脸特征数据的list
				for (String name = bos.readString(); name != null; name = bos.readString()){
				    //读该人名、判断文件夹中是否包含此名字的data文件，有则添加到注册人脸列表（人名）
					if (new File(mDBPath + "/" + name + ".data").exists()) {
						mRegister.add(new FaceRegist(new String(name)));
					}
				}
			}
			bos.close();
			fs.close();
			return true;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}

	public boolean loadFaces(){
		if (loadInfo()) {
		    //第一次加载人脸信息后才会执行此处
			try {
				for (FaceRegist face : mRegister) {
				    //依次提取注册人脸列表（txt中的人名）
					Log.d(TAG, "load name:" + face.mName + "'s face feature data.");
					//找到对应的特征文件
					FileInputStream fs = new FileInputStream(mDBPath + "/" + face.mName + ".data");
					ExtInputStream bos = new ExtInputStream(fs);
					//特征
					AFR_FSDKFace afr = null;
					do {
						if (afr != null) {
							if (mUpgrade) {
								//upgrade data.
							}
							face.mFaceList.add(afr); //添加特征
						}
						afr = new AFR_FSDKFace();//创建一个人脸特征实例
					} while (bos.readBytes(afr.getFeatureData())); //从特征集合文件中读取全部特征
					bos.close();
					fs.close();
					Log.d(TAG, "load name: size = " + face.mFaceList.size());
				}
				return true;
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return false;
	}

	public	void addFace(String name, AFR_FSDKFace face) {
		try {
			//check if already registered.
			boolean add = true;
			for (FaceRegist frface : mRegister) {
				if (frface.mName.equals(name)) {
					frface.mFaceList.add(face);
					add = false;
					break;
				}
			}
			if (add) { // not registered.
				FaceRegist frface = new FaceRegist(name);
				frface.mFaceList.add(face);
				mRegister.add(frface);
			}
            //清空原有txt文件
			if (saveInfo()) {
				//update all names
                //把当前内存里的新数据全部重新添加一次
				FileOutputStream fs = new FileOutputStream(mDBPath + "/face.txt", true);
				ExtOutputStream bos = new ExtOutputStream(fs);
				for (FaceRegist frface : mRegister) {
					bos.writeString(frface.mName);
				}
				bos.close();
				fs.close();

				//save new feature
				fs = new FileOutputStream(mDBPath + "/" + name + ".data", true);
				bos = new ExtOutputStream(fs);
				bos.writeBytes(face.getFeatureData());
				bos.close();
				fs.close();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public boolean delete(String name) {
		try {
			//check if already registered.
			boolean find = false;
			for (FaceRegist frface : mRegister) {
			    //在内存中查找
				if (frface.mName.equals(name)) {
				    //移除特征数据
					File delfile = new File(mDBPath + "/" + name + ".data");
					if (delfile.exists()) {
						delfile.delete();
					}
					//在内存中移除
					mRegister.remove(frface);
					find = true;
					break;
				}
			}

			if (find) {
			    //再次删除txt中的内容
				if (saveInfo()) {
					//update all names
					FileOutputStream fs = new FileOutputStream(mDBPath + "/face.txt", true);
					ExtOutputStream bos = new ExtOutputStream(fs);
					for (FaceRegist frface : mRegister) {
						bos.writeString(frface.mName);
					}
					bos.close();
					fs.close();
				}
			}
			return find;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}

	public boolean upgrade() {
		return false;
	}
}
