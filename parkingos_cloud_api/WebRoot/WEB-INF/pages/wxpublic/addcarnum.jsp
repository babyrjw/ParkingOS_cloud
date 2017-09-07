<!DOCTYPE html>
<%@ page language="java" contentType="text/html; charset=gb2312"
    pageEncoding="gb2312"%>
<html>
<head>
  <meta charset="GB2312">
  <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=1">
<meta content="yes" name="apple-mobile-web-app-capable">
<meta name="apple-mobile-web-app-status-bar-style" content="black">
<meta http-equiv="x-ua-compatible" content="IE=edge">
  <!-- import CSS -->
  <link rel="stylesheet" href="css/weui.min.css">
<link href="css/jquery-weui.min.css" rel="stylesheet">
  <link rel="stylesheet" href="css/mint.css">
  <style type="text/css">
	 .carnum-input{
	 	text-align:center;
	 	height:30px;
	 	width:25px;
	 	font-size:18px;
	 	marigin:0px;
	 	vertical-align:middle; 
	 	border-radius:0px;
	 	text-decoration: none;
	 	border-color:gray;
		border-top-width: 0px;
		border-right-width: 0px; 
		border-bottom-width: 1px;
		border-left-width: 0px;
	 }
	 .info{
	 	display:none
	 }
	 .mint-button--normal {
	    padding: 0 0px;
	    margin:2px 4px;
	    width:46px;
	    text-align:center;
	 }
	 .keyboard{
	 	margin:0px;
	 	padding:0px 1px;
	 	position:absolute;
	 	bottom:0px;
	 	left:0px;
	 	right:0px;
	 	padding:5px 1px;
	 	background:white;
	 }
	  .carnum-select{
	 	border-color:#4BC1CD;
	 	border-bottom-width: 2px;
	 }
	.mint-navbar .mint-tab-item.is-selected {
	    border-bottom: 3px solid #26a2ff;
	    color: #26a2ff;
	}
	</style>
</head>
<body style="background-color:#EEEEEE">
  <div id="app" style="display:none">
  	<mt-navbar v-model="selected">
	  <mt-tab-item id="1"><span style="font-size:17px">���ͳ�</span></mt-tab-item>
	  <mt-tab-item id="2"><span style="font-size:17px">����Դ</span></mt-tab-item>
	</mt-navbar>
  <mt-tab-container>
	  <mt-tab-container-item >
	    <!-- ���복���� -->
		<div align="center" style="padding-top:15px">
			<div style="padding:5px;height:45px;padding-top:8px;border-radius:4px;background:white;">
				<form method="post" role="form" action="${action}" id="checkform">
					<input type="text" name="openid" class="info" value="${openid}">
					<input type="text" id="mobile" name="mobile" class="info" value="${mobile}">
					<input type="text" id="wximg" name="wximg" class="info" value="${wximg}">
					<input type="text" id="wxname" name="wxname" class="info" value="${wxname}">
					<input type="text" id="openid" name="openid" class="info" value="${openid}">
					<input readonly style="vertical-align:middle;border:0px solid;width:55px;text-decoration: none;font-size:17px" value="���ƺ�">
					<input id="carnum1" max-length="1" onfocus="this.blur();" @click="carnum('carnum1')" class="carnum-input"  value="">
					<input id="carnum2" max-length="1" onfocus="this.blur();" @click="carnum('carnum2')" class="carnum-input"  value="">
					<input id="carnum3" max-length="1" onfocus="this.blur();" @click="carnum('carnum3')" class="carnum-input"  value="">
					<input id="carnum4" max-length="1" onfocus="this.blur();" @click="carnum('carnum4')" class="carnum-input"  value="">
					<input id="carnum5" max-length="1" onfocus="this.blur();" @click="carnum('carnum5')" class="carnum-input"  value="">
					<input id="carnum6" max-length="1" onfocus="this.blur();" @click="carnum('carnum6')" class="carnum-input"  value="">
					<input id="carnum7" max-length="1" onfocus="this.blur();" @click="carnum('carnum7')" class="carnum-input"  value="">
					<input id="carnum8" v-show="selected==2" max-length="1" @click="carnum('carnum8')" onfocus="this.blur();" class="carnum-input" value="">
					<input id="carnum9" v-show="false" max-length="1" @click="carnum('carnum9')" onfocus="this.blur();" class="carnum-input" value="">
				</form>
			</div>
		</div>
		 <div style="margin-top:18px" align="center">
		 	<mt-button type="primary" style="width:95%;margin:0px;" @click="confirm">ȷ��</mt-button>
	    </div>
	</mt-tab-container-item>
	</mt-tab-container>
  
  
  
    </br>
    <div class="keyboard" v-show="showChar" align="center">
    
    	<div id="chars">
		    <div style="display:inline;" v-for="char in chars">
		    	<mt-button @click="handleClick(char.name)" :id="char.id">{{char.name}}</mt-button>
		    </div>
	    </div>
    </div>
    
    <div class="keyboard" v-show="showLetter" style="padding:6px 2px;">
    	<div id="letters">
	    	<div align="center">
		    	<div style="display:inline;" v-for="letter in letters">
					<mt-button @click="handleClick(letter.name)">{{letter.name}}</mt-button>	    	
		    	</div>
	    	</div>
	    </div>
    </div>
    	
  </div>
</body>
  <script src="js/jquery.js"></script>
  <script src="js/wxpublic/jquery-weui.min.js"></script>
  <!-- import Vue before Mint UI -->
  <script src="js/vue.js"></script>
  <!-- import JavaScript -->
  <script src="js/mint.js"></script>
  <script>
    new Vue({
      el: '#app',
      data:function(){
      	return{
      		selected:"1",
      		chars:[
      			{"id":"1","name":"��"},
      			{"id":"2","name":"��"},
      			{"id":"3","name":"��"},
      			{"id":"4","name":"��"},
      			{"id":"5","name":"��"},
      			{"id":"6","name":"³"},
      			{"id":"7","name":"��"},
      			{"id":"8","name":"��"},
      			{"id":"9","name":"ԥ"},
      			{"id":"10","name":"��"},
      			{"id":"11","name":"��"},
      			{"id":"12","name":"��"},
      			{"id":"13","name":"��"},
      			{"id":"14","name":"��"},
      			{"id":"15","name":"��"},
      			{"id":"16","name":"��"},
      			{"id":"17","name":"��"},
      			{"id":"18","name":"��"},
      			{"id":"19","name":"��"},
      			{"id":"20","name":"��"},
      			{"id":"21","name":"��"},
      			{"id":"22","name":"��"},
      			{"id":"23","name":"��"},
      			{"id":"24","name":"��"},
      			{"id":"25","name":"��"},
      			{"id":"26","name":"��"},
      			{"id":"27","name":"��"},
      			{"id":"28","name":"��"},
      			{"id":"29","name":"��"},
      			{"id":"30","name":"��"},
      			{"id":"31","name":"��"},
      			{"id":"32","name":"��"},
      			{"id":"33","name":"̨"},
      		],
      		letters:[
      			{"id":"100","name":"0"},
      			{"id":"101","name":"1"},
      			{"id":"102","name":"2"},
      			{"id":"103","name":"3"},
      			{"id":"104","name":"4"},
      			{"id":"105","name":"5"},
      			{"id":"106","name":"6"},
      			{"id":"107","name":"7"},
      			{"id":"108","name":"8"},
      			{"id":"109","name":"9"},
      			{"id":"50","name":"A"},
      			{"id":"51","name":"B"},
      			{"id":"52","name":"C"},
      			{"id":"53","name":"D"},
      			{"id":"54","name":"E"},
      			{"id":"55","name":"F"},
      			{"id":"56","name":"G"},
      			{"id":"57","name":"H"},
      			{"id":"58","name":"J"},
      			{"id":"59","name":"K"},
      			{"id":"60","name":"L"},
      			{"id":"61","name":"M"},
      			{"id":"62","name":"N"},
      			{"id":"63","name":"P"},
      			{"id":"64","name":"Q"},
      			{"id":"65","name":"R"},
      			{"id":"66","name":"S"},
      			{"id":"67","name":"T"},
      			{"id":"68","name":"U"},
      			{"id":"69","name":"V"},
      			{"id":"70","name":"W"},
      			{"id":"71","name":"X"},
      			{"id":"72","name":"Y"},
      			{"id":"73","name":"Z"},
      			{"id":"99","name":"��ɾ"},
      			{"id":"98","name":"�ر�"},
      		],
      		keyboard:'',
      		btn:'',
      		showChar:'',
      		showLetter:'',
      		selected1:'',
      		selected2:'',
      		selected3:'',
      		selected4:'',
      		selected5:'',
      		selected6:'',
      		selected7:'',
      		selected8:'',
      		cNode:'carnum1',
      		
      	}
      	
      },
      methods: {
        handleClick: function(name) {
            console.log(name)
            var value = name
            if(name=='�ر�'){
            	this.showLetter=false;
            	this.showChar=false;
            }else if(name=='��ɾ'){
            	//�����ǰ�ڵ�ֵ
				var cur = document.getElementById(this.cNode)
				this.removeClass(cur, "carnum-select")
				var i = parseInt(this.cNode.substr(6))-1;
				if(i==1){
					//��ʾ����,������ĸ
					this.showChar=true;
					this.showLetter=false;
				}else if(i<1){
					i=1
				}
				this.cNode = "carnum"+i
				var last = document.getElementById(this.cNode)
				last.value = "";
				this.addClass(document.getElementById(this.cNode),"carnum-select")
            }else{
            	//
            	var cur = document.getElementById(this.cNode)
				var i = parseInt(this.cNode.substr(6))
				if(this.selected=="1"){
					if(i>=8){
						i=8
					}else{
						cur.value = value;
						i+=1;
					}
				}else{
					if(i>=9){
						i=9
					}else{
						cur.value = value;
						i+=1;
					}
				}
				
				this.removeClass(cur, "carnum-select")
				this.cNode = "carnum"+i
				this.addClass(document.getElementById(this.cNode),"carnum-select")
				if(i==2){
					this.showChar=false;
					this.showLetter=true;
				}
            }
            
            
        },
        carnum:function(id){
			var k =id.substr(6)
			this.cNode = id;
			for(var i=1;i<=8;i++){
				if(i==k){
					this.addClass(document.getElementById("carnum"+i),"carnum-select")
				}else{
					this.removeClass(document.getElementById("carnum"+i),"carnum-select")
				}
			}
			console.log(k)
			if(k=='1'){
				//��ʾ����,������ĸ
				this.showChar=true;
				this.showLetter=false
			}else{
				//��֮
				this.showChar=false;
      	        this.showLetter=true    
			}
        },
        confirm:function(){
        	//��ȡ��������
			var sum = "7"
			var carnumber = ""
			if(this.selected=="2"){
				sum = "8"
			}
			var submitable = true;
			for(var i=1;i<=sum;i++){
				var carnum = document.getElementById("carnum"+i)
				if(i=="2"){
					var m = /^[A-Z]{1}$/;
					if(!carnum.value.match(m)){
						$.alert("���ƺŲ���ȷ!")
						submitable = false;
						break;
					}
				}
				if(carnum.value==""||typeof(carnum.value)=="undefined"){
					$.alert("���ƺŲ���ȷ!")
					submitable = false;
					break;
				}
				carnumber += carnum.value
			}
			console.log(carnumber)
			if(submitable){
				carnumber = encodeURI(carnumber);
				this.uploadcnum(carnumber)				
			}
        
        },
        uploadcnum:function(carnumber){
        	$.showLoading("�ϴ���,���Ժ�...");
			jQuery.ajax({
					type : "post",
					url : "wxpaccount.do",
					data : {
						'openid' : '${openid}',
						//'carid' : '${carid}',
						'carnumber' : carnumber,
						'action' : 'upload',
					},
					//async : false,
					success : function(result) {
						if(result == "-1"){
							setTimeout('$.hideLoading();$.alert("�������ύ")',500)
						}else if(result == "-2"){
							setTimeout('$.hideLoading();$.alert("�ó����ѱ�ע��<br>�ڹ��ں��ڵ������ϵ�ͷ������")',500)
						}else if(result == "-3"){
							setTimeout('$.hideLoading();$.alert("����ע��ó���!")',500)
						}else if(result == "-4"){
							setTimeout('$.hideLoading();$.alert("��������������")',500)
						}else{
							$("#checkform")[0].submit();
						}
					}
				});
        },
        
        //ÿ�����һ��class
		addClass:function(currNode, newClass){
	        var oldClass;
	        oldClass = currNode.getAttribute("class") || currNode.getAttribute("className");
	        if(oldClass !== null) {
			   newClass = oldClass+" "+newClass; 
			}
			currNode.className = newClass; //IE ��FF��֧��
   		},
		//ÿ���Ƴ�һ��class
		removeClass:function(currNode, curClass){
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
      },
      mounted:function(){
      	 var w = screen.width;
      	 this.keyboard = "width:"+w*0.95+"px"
      	 document.getElementById('app').style.display = 'block'
      	 //��ʾ��������,������ĸ
      	 this.showChar=true;
      	 this.showLetter=false
      	 //inputѡ��1
      	 var carnum1 = document.getElementById("carnum1");
      	 this.addClass(carnum1,"carnum-select")
      }
    })
  </script>
</html>