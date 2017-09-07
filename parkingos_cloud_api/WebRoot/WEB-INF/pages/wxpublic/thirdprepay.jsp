<%@ page language="java" contentType="text/html; charset=gb2312"
    pageEncoding="gb2312"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta content="width=device-width,initial-scale=1.0,maximum-scale=1.0,user-scalable=no" name="viewport">
<meta content="yes" name="apple-mobile-web-app-capable">
<meta content="black" name="apple-mobile-web-app-status-bar-style">
<meta content="telephone=no" name="format-detection">
<meta content="email=no" name="format-detection">
<title>${title}</title>
<script src="js/jquery.js" type="text/javascript"></script>
<link rel="stylesheet" href="css/prepay.css?v=2">
<link rel="stylesheet" href="css/weui-0.4.3.css">
<link rel="stylesheet" href="css/jquery-weui-0.8.3.css">
<style type="text/css">
.error {
	color: red;
	font-size: 15px;
	margin-top:5%;
}
.wx_pay{
	border-radius:5px;
	width:96%;
	margin-left:2%;
	height:40px;
	margin-top:5%;
	font-size:15px;
	background-color:#04BE02;
	color:white;
}

</style>
<script src="js/wxpublic/jquery-weui.min.js"></script>
<script type="text/javascript">
			//ÿ�����һ��class
	function addClass(currNode, newClass){
        var oldClass;
        oldClass = currNode.getAttribute("class") || currNode.getAttribute("className");
        if(oldClass !== null) {
		   newClass = oldClass+" "+newClass; 
		}
		currNode.className = newClass; //IE ��FF��֧��
  		}
	
	//ÿ���Ƴ�һ��class
	function removeClass(currNode, curClass){
		var oldClass,newClass1 = "";
        oldClass = currNode.getAttribute("class") || currNode.getAttribute("className");
        if(oldClass !== null) {
		   oldClass = oldClass.split(" ");
		   for(var i=0;i<oldClass.length;i++){
			   if(oldClass[i] != curClass){
				   if(newClass1 == ""){
					   newClass1 += oldClass[i]
				   }else{
					   newClass1 += " " + oldClass[i];
				   }
			   }
		   }
		}
		currNode.className = newClass1; //IE ��FF��֧��
	}
	
	//����Ƿ������ǰclass
	function hasClass(currNode, curClass){
		var oldClass;
		oldClass = currNode.getAttribute("class") || currNode.getAttribute("className");
		if(oldClass !== null){
			oldClass = oldClass.split(" ");
			for(var i=0;i<oldClass.length;i++){
			   if(oldClass[i] == curClass){
				   return true;
			   }
		   }
		}
		return false;
	}
</script>
</head>
<body>
	<dl class="my-lpn">
		<dt class="title">�ҵĳ��ƺ���</dt>
		<dd class="lpn"><span>${carnumber}</span><span id ="mycar"><a class="change-btn" href="wxpfast.do?action=thirdeditcar&openid=${openid}&comid=${comid}&ordeid=${orderid}">�޸�</a></span></dd>
	</dl>
	<section class="main" >
		<fieldset id = "maindiv">
		
		<div class="info-area">	
			<dl class="totle" style="border-bottom:0px">
				<dt class="totle-title" id="pay_title">Ԥ��ͣ����</dt>
				<dd class="totle-num" style="color:#04BE02;" id ="pay_money">��<span id="money">${money}</span></dd>
				<div class="sweepcom hide" style="border-bottom: 1px solid #E0E0E0;"></div>
			</dl>
			<ul class="info-list" style="padding-top:1px;">
				<li class="list" id='prepayli'><span class="list-title">�Ѹ����</span><span class="list-content" id="prepaymoney">${prepay}Ԫ</span></li>
				<li class="list" id='parknameli'><span class="list-title" >��������</span><span class="list-content">${parkname}</span></li>
				<li class="list"><span class="list-title">�볡ʱ��</span><span class="list-content">${starttime}</span></li>
				<li class="list" id="prepaidtime_title"><span class="list-title" id="prepaidtime" >��ͣʱ��</span><span class="list-content" id="parktime" >${parktime}</span></li>
				<li class="list"><span class="list-title">���ƺ���</span><span class="list-content">${carnumber}</span></li>
			</ul>
			</ul>
		</div>
		<div style="height:15px"></div>
		<input type="button" id="wx_pay" onclick='payorder();' class="weui_btn weui_btn_primary" style="width:95%" value="ȥ֧��">
		<div class="tips"></div>
		</fieldset>
		<div style="text-align:center;" id="error" class="error">10����֮���볡���</div>
		<div class="wxpay-logo hide"></div>
	</section>
</body>
<script type="text/javascript">
	var carnumber= "${carnumber}";
	var car_number=encodeURI(carnumber);
	var orderid = "${orderid}";//�������
	var state = "${state}";
	var prepay = "${prepay}";//�Ѹ����
	var parkname="${parkname}";
	var clientType = "${client_type}";
	
	
	var money = '${money}';
	//alert(state);
	var getObj =function(id){return document.getElementById(id)};
	if(prepay==0){
		getObj("prepayli").style.display='none';
	}
	if(clientType=='ali'){
		getObj("mycar").style.display='none';
	}
		
	if(parkname==''){
		getObj("parknameli").style.display='none';
	}
	//state 0ʧ�ܣ�1�ɹ�,2��Ԥ����3���粻ͨ
	if(state == "0"){
		getObj("maindiv").style.display='none';
		getObj("error").innerHTML = "��ǰ�޶���";
	}else if(state==2){
		$(".weui_btn").addClass("hide");
		getObj("pay_title").innerHTML="��Ԥ������";
		getObj("prepaidtime").innerHTML="��Ԥ��ʱ��";
		getObj("error").innerHTML = "����Ԥ֧�����������ٴ�Ԥ֧��";
	}else if(state==3){
		$(".weui_btn").addClass("hide");
		getObj("pay_money").innerHTML="";
		getObj("parktime").innerHTML="";
		//getObj("pay_title").innerHTML="���δ֪";
		getObj("error").innerHTML = "��ѯ���ʧ�ܣ����Ժ�����";
	}
	
	if(parseInt("${free_out_time}")>0){
		getObj("error").innerHTML = "${free_out_time}����֮���볡���";
	}
	
	if("${parktime}"=="0"){
		getObj("prepaidtime_title").style.display='none';
	}
	
	if(parseFloat('${money}')==0){
		$(".weui_btn").addClass("hide");
	}
	
	function payorder(){
		if(clientType=='ali'){
			location="aliprepay.do?action=prepay&uin=${uin}&unionid=&parkid=${comid}&bid=${oid}&orderid="+orderid;
			return ;
		}
		jQuery.ajax({
			type : "post",
			url : "wxpfast.do",
			data : {
				'orderid' : orderid,
				'openid' : '${openid}',
				'oid' : '${oid}',
				'money' : money,
				'car_number' : car_number,
				'parkid' : '${comid}',
				'uin' : '${uin}',
				'test' : '0',//���Ա�־
				'action' : 'thirdweixinorder'
			},
			async : false,
			success : function(result) {
				if(result){
					var rest = eval(''+result+'');
					var state = rest[0].state;
					if(state ==1){//��������֧��
							var appid = rest[0].appid;
							var timestamp = rest[0].timestamp;
							var nonceStr = rest[0].nonceStr;
							var packagevalue = rest[0].packagevalue;
							var signType = rest[0].signType;
							var paySign = rest[0].paySign;
							callpay(appid,timestamp,nonceStr,packagevalue,signType,paySign);
					}else if(state==2){//��������б仯
						var nowmoney =rest[0].money;
						//$(".weui_btn").addClass("hide");
						//getObj("error").innerHTML = "��������б仯��ԭ���"+premoney+"�����ڽ��"+nowmoney;
						money = nowmoney;
						getObj("money").innerHTML=nowmoney;
						var duration = rest[0].duration;
						if(duration){
							getObj("prepaidtime_title").style.display='';
							getObj("parktime").innerHTML=duration;
						}
						getObj("wx_pay").value="����֧��";
					}else if(state==3){//��Ԥ��
						var prepayid = rest[0].prepay;
						$(".weui_btn").addClass("hide");
						getObj("prepayli").style.display='';
						getObj("prepaymoney").innerHTML = prepayid+"Ԫ";
					}else{//���� ���µ����� 
						$(".weui_btn").addClass("hide");
						getObj("error").innerHTML = "֧������������ɨ��";
					}
				}
			}
		});
	}
	function callpay(appid,timestamp,nonceStr,packagevalue,signType,paySign){//����΢��֧��
		 WeixinJSBridge.invoke(
				 'getBrandWCPayRequest',
				 {
					 "appId" : appid,
	          		 "timeStamp":timestamp,
	           		 "nonceStr" : nonceStr,
	           		 "package" : packagevalue,
	           		 "signType" :signType,
	           		 "paySign" :paySign
				 },
				 function(res){
           	 if(res.err_msg == "get_brand_wcpay_request:ok"){
//           		 alert("��ʼ��ת�ɹ�ҳ�档����");
           		 window.location.href = "wxpublic.do?action=balancepayinfo&openid=${openid}&money="+money+"&notice_type=${notice_type}&leaving_time=${delaytime}&paytype=${paytype}&orderid=${orderid}";
           	 }
           });
	}
	//*****************************end************************//
</script>
</html>
