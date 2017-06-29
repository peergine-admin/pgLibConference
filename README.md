

# 怎样测试DEMO

    1、在安卓设备中安装Peergine中间件,安装包为：pgPlugin_XXXXX.apk。
	
	2、然后在安卓设备中安装Demo程序，安装包为DemoApp/Demo_XXXXXX.apk。
	
	3、重复步骤1和步骤2,安装到其他安卓设备。
	
	4、根据界面面提示在所有安卓设备中输入相同会议ID，其他一台启动为主席（Chairman），其他为非主席。
	
	5、查看视频连接效果。

	
# 怎样使用会议模块开发APP
	
	1、使用AndroidStudio创建工程。
	
	2、将 pgLibConference 作为模块(Module)导入你创建的工程。
	
	3、将 pgPluginLib 作为模块(Module)导入你创建的工程。（Tips: 如果要导入其他带SO的库，请保持平台一致，具体做法是如果其他SO库只有32位的，就将我们的64位的SO删除。如果其他SO库只有64位的就将我们的32位的删除。也可以补全不同位数的so库）。

	4、在你的 Module 中添加对 pgLibConference pgPluginLib 的依赖 (在Android Studio中选定生成 app 的 Module ，点击菜单Build->Edit Libraries and Dependencies,在弹出的对话框左上角有个绿色的+号，点击选定Module Dependency).
	
	5、可以将Demo导入你的工程，参考
	private pgLibConference.OnEventListener m_OnEvent = new pgLibConference.OnEventListener() {
		@Override
		public void event(String sAct, String sData, final String sPeer) {
		}
	}。
	构建事件接受器。我们的库和NodeJs一样都是事件驱动的。
	
	
# 版本升级日志
## v19
 优化了部分性能。

##    v1.0.5
    1.添加视频外部采集接口参数VideoInExternal
    2.视频流角度调整参数。
    3.添加API给服务器发送扩展消息
    4.添加事件PeerSync表示本端与对端建立通道，可以利用MessageSend进行通信。
    5.添加事件PeerOffline表示对端已经离线。
    6.添加事件VideoLost用来报告视频已经丢失
    7.内部添加成员端对主席端的心跳。以加快将网络异常报告给应用程序。
    8.内部添加视频连接时的心跳，以检测异常重启时对端不能及时获取视频丢失的消息。
    进行了若干优化。修复了若干BUG。

## updata 2016/11/17 v1.0.6
     1、添加视频的抓拍和录制功能
     2、做了一个超时检测 在执行MemberAdd MemberDel Leave 操作是 如果45秒内没有退出和加入会议   。就产生TimeOut 的回调    sData 数操作名   sPeer是参数
     3、还添加了CallSend 功能比较MessageSend会产生CallSend的回执
     4、CallSend函数的最后一个参数自定义
     5、CallSend回调事件的sData 是错误代码 0是正常 ，sPeer是CallSend的最后一个参数

## updata 2016/12/30 v1.25.1.9
     1、升级产品版本规则，版本号前3位是中间件版本，后一位是SDK版本
     2、升级打包规则，不同平台分别打包
     3、updata增加一些视音频操作函数，节点操作函数 ，Reset 函数 等
     4、增加音频初始化选项

## updata 2016/12/30 v1.25.1.10
     1、升级AudioSpeech函数，增加一个参数，同时兼容之前的函数。


## updata 2017/02/014 v13
     继续优化心跳包。
     删除会议模块在离线事件和离开会议后的主动清理视频的代码。
     修复上报离线事件后再次连接上报同步消息。
     修复主席端对同一节点反复上报离线消息。
     修复反复上报VideoLost消息。
     修改PG_PEER 的列表添加位置，由加入会议添加，离开会议删除，改为视频打开或收到请求添加，视频关闭删除
     修改函数VideoOpen中对同一节点的Node和View，由新建改为继承。
     修改Keep函数中的列表的遍历方式。
     修改结构，使得可以在SDK Initialze 后可以在之后任意位置VideoStart 和AudioStart
     函数执行打印信息

     开放简易定时器
        相关接口TimerOut
     相关函数：
        TimerOutAdd  把接口TimerOut的实现加入定时器处理
        TimerOutDel  把接口TimerOut的实现从定时器处理中删除
        TimerStart  开始一个定时器处理
        TimerStop  对定时时间长或者循环定时进行停止操作

## updata 2017/02/014 v14
    1、取消TimeOut事件的上报。
       如 ：Act="TimeOut",sData = "MemberAdd",sPeer 等不再上报。
    2 、取消利用临时用户登录代码
    3、取消bOpened的使用
    4、增加Config_Node函数，在初始化前配置初始化参数。输入参数为结构体PG_NODE_CFG,具体情况可查看该结构体的注释。

## updata 2017/03/01 v15
    1 增加关于CallSend 的log打印。
    2 修复CallSend偶尔收不到回执CallSend事件的异常。
    3 优化其他问题。

##  updata 2017/4/7 v16
    添加功能：
        添加重载Initialze方法，增加一种初始化方式，初始化后只登陆，而不开始会议。原有Initialze 初始化登陆的同时开始会议。
        增加Start 方法，会议没有开始时，开始会议。初始化会议相关的视音频数据通道。
        增加Stop 方法，会议的相关逻辑，停止会议。清理会议视音频，消息发送等相关数据。
    升级功能：
        升级SetExpire 方法 设置心跳间隔，当设置为0时 关闭心跳。 请保持各端心跳设置一致，如果不同可能导致误报离线状态。
    其他升级：
        对SDK进行了数据结构的优化。
        现在会议没有Start也能发消息。
        修复录音录像不能停止的问题。
        升级中间件。
    已知问题：


## updata 2017/4/24 v17
     fix：

     修复一些空指针错误。

     修改：
          VideoRecordStart 改成只能录制视频，
          AudioRecordStart 改成只能录制音频，
          VideoRecord 添加一个参数用来指定是否录制是包含音频，这个函数在未来某个版本将可能不在公开
          AudioRecord 添加一个参数用来指定是否录制是包含视频，这个函数在未来某个版本将可能不在公开
     添加：
          RecordStart 为开始录制视音频
          RecordStop 为停止录制视音频


##  updata 2017/5/12 v18
    修复一些已知问题。
    添加：
        函数 LanScanStart 检测局域网中的会议设备，通过LanScanResult 事件上报局域网中的会议设备及其地址。
        功能：如果在路由器没有接入公网的情况下，Start后可以使用LanScanStart 成员端可以连上主席端进行对讲。
        如果主席端和成员端在同一局域网的，并且成员端通过LanScanStart扫描到了主席端，当外网断开的情况下，主席端和成员端处于Start状态下。
        主席端和成员端可以保持连接。可以继续对讲和通信
