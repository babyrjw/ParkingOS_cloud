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
<title>���복��</title>
<script src="js/jquery.js" type="text/javascript"></script>
<link rel="stylesheet" href="css/prepay.css?v=3">
<style type="text/css">
.error {
	color: red;
	font-size: 15px;
	margin-top:5%;
}
.noorder{
	text-align:center;
	color:red;
	margin-top:55%;
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
select{
-webkit-appearance: none;
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

.hide{
	display:none;
}
</style>

</head>
<body>

	<section class="main">
		<!-- ���복�ƺ�[[ -->
		<form method="post" action="${action}" role="form" class="form lpn-form" id="carnumberform">
			<fieldset>
				
				<div class="input-area">
					<dl class="form-line">
						<dt class="label">���ƺ���</dt>
						<dd class="element lpn-element">
							<input class="text" type="text" name="carnumber" id="carnumber" placeholder="�����복�ƺ�" maxlength="8">
						</dd>
					</dl>

				</div>

				<div class="form-tips">������˳��ƺţ��´���������</div>
				<input type="button" id="wx_pay" onclick='check();' class="wx_pay" value="ȷ��">
				<input type="text" name="park_id" value="${comid}" class="hide">
				<input type="text" name="unionid" value="${unionid}" class="hide">
				<input type="text" name="order_id" value="${orderid}" class="hide">
			</fieldset>
		</form>
		<!-- ���복�ƺ�]] -->
		<div style="text-align:center;" id="error" class="error"></div>
	</section>
<script type="text/javascript">
	function check(){
		var carnumber = document.getElementById("carnumber").value;
		carnumber = carnumber.toUpperCase();
		var city = carnumber.charAt(0);
		var array = new Array( "��", "��", "��", "��", "��", "³",
					"��", "��", "ԥ", "��", "��", "��", "��", "��", "��", "��", "��", "��",
					"��", "��", "��", "��", "��", "��", "��", "��", "��", "��", "��", "��",
					"��", "��", "��", "ʹ", "��", "��", "��", "��", "��", "��","��", "��", "��", "��", "WJ", "��", "��", "��","ˮ", "��", "��", "ͨ" );  
		var m = /^([A-Z]{1}[A-Z_0-9]{5})|([A-Z]{1}[A-Z_0-9]{6})$/;
		carnumber_char = carnumber.substr(1);
		if(array.toString().indexOf(city) > -1){
			if(city == "ʹ"){
				m = /^[A-Z_0-9]{6}$/;
			}
			if(!carnumber_char.match(m)){
				document.getElementById("error").innerHTML = "���ƺŲ���ȷ";
				return false;
			}
		}else{
			document.getElementById("error").innerHTML = "���ƺŲ���ȷ";
			return false;
		}
		//carnumber = carnumber.toUpperCase();
		document.getElementById("carnumber").value=carnumber;
		carnumber = encodeURI(carnumber);
		alert('���ƴ���cookie');
		var Days = 100;
		var exp = new Date();
		exp.setTime(exp.getTime() + Days*24*60*60*1000);
		document.cookie = "lience="+ carnumber + ";expires=" + exp.toGMTString();
		$("#carnumberform")[0].submit();
	} 
	
</script>
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
</body>
</html>
