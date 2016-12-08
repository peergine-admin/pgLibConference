

#怎样测试DEMO

    1、在安卓设备中安装Peergine中间件,安装包为：pgPlugin_XXXXX.apk。
	
	2、然后在安卓设备中安装Demo程序，安装包为DemoApp/Demo_XXXXXX.apk。
	
	3、重复步骤1和步骤2,安装到其他安卓设备。
	
	4、根据界面面提示在所有安卓设备中输入相同会议ID，其他一台启动为主席（Chairman），其他为非主席。
	
	5、查看视频连接效果。

	
#怎样使用会议模块开发APP
	
	1、使用AndroidStudio创建工程。
	
	2、将 pgLibConference 作为模块(Module)导入你创建的工程。
	
	3、将 pgPluginLib 作为模块(Module)导入你创建的工程。

	4、在你的 Module 中添加对 pgLibConference pgPluginLib 的依赖 (在Android Studio中选定生成 app 的 Module ，
点击菜单Build->Edit Libraries and Dependencies,在弹出的对话框左上角有个绿色的+号，点击选定Module Dependency).
	
	5、可以将Demo导入你的工程，参考
	private pgLibConference.OnEventListener m_OnEvent = new pgLibConference.OnEventListener() {
		@Override
		public void event(String sAct, String sData, final String sPeer) {
		}
	}。
	构建事件接受器。我们的库和NodeJs一样都是事件驱动的。
	
	
v1.0.5
1.添加视频外部采集接口参数VideoInExternal
2.视频流角度调整参数。
3.添加API给服务器发送扩展消息
4.添加事件PeerSync表示本端与对端建立通道，可以利用MessageSend进行通信。
5.添加事件PeerOffline表示对端已经离线。
6.添加事件VideoLost用来报告视频已经丢失
7.内部添加成员端对主席端的心跳。以加快将网络异常报告给应用程序。
8.内部添加视频连接时的心跳，以检测异常重启时对端不能及时获取视频丢失的消息。
进行了若干优化。修复了若干BUG。

updata 2016/11/17 v1.0.6
 * 添加视频的抓拍和录制功能
 * 做了一个超时检测 在执行MemberAdd MemberDel Leave 操作是 如果45秒内没有退出和加入会议   。就产生TimeOut 的回调    sData 数操作名   sPeer是参数
 还添加了CallSend 功能比较MessageSend会产生CallSend的回执
 CallSend函数的最后一个参数自定义
 CallSend回调事件的sData 是错误代码 0是正常 ，sPeer是CallSend的最后一个参数