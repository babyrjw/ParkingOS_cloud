var OFFSET = 5;
var page = 1;
var PAGESIZE = 9999;

var myScroll,
	pullDownEl, pullDownOffset,
	pullUpEl, pullUpOffset,
	generatedCount = 0;
var maxScrollY = 0;

var hasMoreData = false;
var today = new Date();
today.setHours(0);
today.setMinutes(0);
today.setSeconds(0);
today.setMilliseconds(0);
today = today/1000 + 24*60*60;//�����ʼʱ��
var openid;

document.addEventListener('touchmove', function(e) {
	e.preventDefault();
}, false);

document.addEventListener('DOMContentLoaded', function() {
	$(document).ready(function() {
		//var mobile=$("#mobile")[0].value;
		openid=$("#openid")[0].value;
		loaded(openid);
	});
}, false);

function loaded(openid) {
	pullDownEl = document.getElementById('pullDown');
	pullDownOffset = pullDownEl.offsetHeight;
	pullUpEl = document.getElementById('pullUp');
	pullUpOffset = pullUpEl.offsetHeight;

	hasMoreData = false;
	// $("#thelist").hide();
	$("#pullUp").hide();

	pullDownEl.className = 'loading';
	pullDownEl.querySelector('.pullDownLabel').innerHTML = '������...';

	page = 1;
	$.post("carowner.do", {
			"page": page,
			"pagesize": PAGESIZE,
			"openid" : openid,
			"action" : "products"
		},
		function(response, status) {
			if (status == "success") {
				$("#thelist").show();
				if (response.length < PAGESIZE) {
					hasMoreData = false;
					$("#pullUp").hide();
				} else {
					hasMoreData = true;
					$("#pullUp").show();
				}

				// document.getElementById('wrapper').style.left = '0';

				myScroll = new iScroll('wrapper', {
					useTransition: true,
					topOffset: pullDownOffset,
					onRefresh: function() {
						if (pullDownEl.className.match('loading')) {
							pullDownEl.className = 'idle';
							pullDownEl.querySelector('.pullDownLabel').innerHTML = '����ˢ��...';
							this.minScrollY = -pullDownOffset;
						}
						if (pullUpEl.className.match('loading')) {
							pullUpEl.className = 'idle';
							pullUpEl.querySelector('.pullUpLabel').innerHTML = '�������ظ���...';
						}
					},
					onScrollMove: function() {
						if (this.y > OFFSET && !pullDownEl.className.match('flip')) {
							pullDownEl.className = 'flip';
							pullDownEl.querySelector('.pullDownLabel').innerHTML = '���ֿ�ʼˢ��...';
							this.minScrollY = 0;
						} else if (this.y < OFFSET && pullDownEl.className.match('flip')) {
							pullDownEl.className = 'idle';
							pullDownEl.querySelector('.pullDownLabel').innerHTML = '����ˢ��...';
							this.minScrollY = -pullDownOffset;
						} 
						if (this.y < (maxScrollY - pullUpOffset - OFFSET) && !pullUpEl.className.match('flip')) {
							if (hasMoreData) {
								this.maxScrollY = this.maxScrollY - pullUpOffset;
								pullUpEl.className = 'flip';
								pullUpEl.querySelector('.pullUpLabel').innerHTML = '���ֿ�ʼˢ��...';
							}
						} else if (this.y > (maxScrollY - pullUpOffset - OFFSET) && pullUpEl.className.match('flip')) {
							if (hasMoreData) {
								this.maxScrollY = maxScrollY;
								pullUpEl.className = 'idle';
								pullUpEl.querySelector('.pullUpLabel').innerHTML = '�������ظ���...';
							}
						}
					},
					onScrollEnd: function() {
						if (pullDownEl.className.match('flip')) {
							pullDownEl.className = 'loading';
							pullDownEl.querySelector('.pullDownLabel').innerHTML = '������...';
							// pullDownAction(); // Execute custom function (ajax call?)
							refresh();
						}
						if (hasMoreData && pullUpEl.className.match('flip')) {
							pullUpEl.className = 'loading';
							pullUpEl.querySelector('.pullUpLabel').innerHTML = '������...';
							// pullUpAction(); // Execute custom function (ajax call?)
							nextPage();
						}
					}
				});

				$("#thelist").empty();
				$.each(response, function(key, value) {
					var cid = value.cid;//�ƶ��¿���¼���
					var comid = value.comid;
					var cardid = value.cardid;
					var carnumber = value.carnumber;
					var mid = value.mid;
					var state = value.state;//0��δ��ʼ 1:ʹ���� 2�ѹ���
					var cname = value.name;//ͣ��������
					var pname = value.parkname;//�ײ�����
					var price = value.price;//�ײ͵���
					var prodid = value.prodid;
					//var cid = value.cid;//�ײ͵���
					var limitdate = "��Ч�� " + value.limitdate;//��Ч��
					//var limittime = "����ʱ�Σ�"+ value.limittime;//��Чʱ��
					var limitday = value.limitday;//ʣ������
					var isthirdpay = value.isthirdpay;//�Ƿ�ȥ������֧��
					var money_class = "money";
					var ticketname_class = "ticketname";
					var ticketinfo_class = "ticketinfo";
					var ticketlimit_class = "normal";
					var line_class = "line";
					var useinfo_class = "useinfoused";
					
					var guoqi = "δʹ��";
					if(state == 1){
						guoqi = "ʹ����";
					}else if(state == 2){
						guoqi = "�ѹ���";
						money_class = "moneyused";
						ticketname_class = "ticketnameused";
						ticketinfo_class = "ticketinfoused";
						ticketlimit_class = "normalused";
						line_class = "lineuesd";
						useinfo_class = "useinfoexp";
					}
					//var click=' onclick="rewand('+prodid+','+cid+')"';
					var click=' onclick="rewand(\''+cardid+'\',\''+prodid+'\',\''+cid+'\',\''+comid+'\',\''+isthirdpay+'\')"';
					$("#thelist").append('<li '+click+' class="li1"><div class="moneyouter"><span class="'+money_class+'">'+price+'<span class="fuhao">Ԫ</span></span></div><a class="a1" href="#"><div class="'+ticketname_class+'">'+
							cname+'</div><div class="ticketlimit"><div style="height:10px"></div><span class="sel_fee '+ticketlimit_class+'">'+pname+'</span></div><div style="height:8px"></div><div class="ticketlimit2"><span>'+carnumber+'</span></div></a><div class="rewand">����</div></li>');
					$("#thelist").append('</div><li class="li2"><div style="height:5px"><div class="'+line_class+'"></div><a class="a2" href="#"><div class="'+useinfo_class+'">'+guoqi+'</div><div class="limittime">'+limitdate+'</div></a></li>');
					
				});
				
				myScroll.refresh(); 
				if(response.length == 0){
					$(".middle").removeClass("hide");
				}
				if (hasMoreData) {
					myScroll.maxScrollY = myScroll.maxScrollY + pullUpOffset;
				} else {
					myScroll.maxScrollY = myScroll.maxScrollY;
				}
				maxScrollY = myScroll.maxScrollY;
			};
		},
		"json");
}
function rewand(cardid,prodid,cid,comid,isthirdpay){
	isthirdpay = 0
	if(isthirdpay==1){
		var url = "https://s.bolink.club/unionapi/toqrcode"//park_id="++"&union_id="+
	}else{
		var url = "wxpaccount.do?action=tobuyprod&openid="+openid+"&cardid="+cardid+"&prodid="+prodid+"&cid="+cid+"&comid="+comid+"&type=1";
	}
	window.location.href = url;
}
function refresh() {
	var openid=$("#openid")[0].value;
	page = 1;
	$.post("carowner.do", {
		"page": page,
		"pagesize": PAGESIZE,
		"openid" : openid,
		"action" : "products"
	},
	function(response, status) {
		if (status == "success") {
			$("#thelist").show();

			if (response.length < PAGESIZE) {
				hasMoreData = false;
				$("#pullUp").hide();
			} else {
				hasMoreData = true;
				$("#pullUp").show();
			}

			// document.getElementById('wrapper').style.left = '0';

			myScroll = new iScroll('wrapper', {
				useTransition: true,
				topOffset: pullDownOffset,
				onRefresh: function() {
					if (pullDownEl.className.match('loading')) {
						pullDownEl.className = 'idle';
						pullDownEl.querySelector('.pullDownLabel').innerHTML = '����ˢ��...';
						this.minScrollY = -pullDownOffset;
					}
					if (pullUpEl.className.match('loading')) {
						pullUpEl.className = 'idle';
						pullUpEl.querySelector('.pullUpLabel').innerHTML = '�������ظ���...';
					}
				},
				onScrollMove: function() {
					if (this.y > OFFSET && !pullDownEl.className.match('flip')) {
						pullDownEl.className = 'flip';
						pullDownEl.querySelector('.pullDownLabel').innerHTML = '���ֿ�ʼˢ��...';
						this.minScrollY = 0;
					} else if (this.y < OFFSET && pullDownEl.className.match('flip')) {
						pullDownEl.className = 'idle';
						pullDownEl.querySelector('.pullDownLabel').innerHTML = '����ˢ��...';
						this.minScrollY = -pullDownOffset;
					} 
					if (this.y < (maxScrollY - pullUpOffset - OFFSET) && !pullUpEl.className.match('flip')) {
						if (hasMoreData) {
							this.maxScrollY = this.maxScrollY - pullUpOffset;
							pullUpEl.className = 'flip';
							pullUpEl.querySelector('.pullUpLabel').innerHTML = '���ֿ�ʼˢ��...';
						}
					} else if (this.y > (maxScrollY - pullUpOffset - OFFSET) && pullUpEl.className.match('flip')) {
						if (hasMoreData) {
							this.maxScrollY = maxScrollY;
							pullUpEl.className = 'idle';
							pullUpEl.querySelector('.pullUpLabel').innerHTML = '�������ظ���...';
						}
					}
				},
				onScrollEnd: function() {
					if (pullDownEl.className.match('flip')) {
						pullDownEl.className = 'loading';
						pullDownEl.querySelector('.pullDownLabel').innerHTML = '������...';
						// pullDownAction(); // Execute custom function (ajax call?)
						refresh();
					}
					if (hasMoreData && pullUpEl.className.match('flip')) {
						pullUpEl.className = 'loading';
						pullUpEl.querySelector('.pullUpLabel').innerHTML = '������...';
						// pullUpAction(); // Execute custom function (ajax call?)
						nextPage();
					}
				}
			});

			$("#thelist").empty();
			$.each(response, function(key, value) {
				var cid = value.cid;//�ƶ��¿���¼���
				var cardid = value.cardid;
				var carnumber = value.carnumber;
				var mid = value.mid;
				var state = value.state;//0��δ��ʼ 1:ʹ���� 2�ѹ���
				var cname = value.name;//ͣ��������
				var pname = value.parkname;//�ײ�����
				var price = value.price;//�ײ͵���
				var prodid = value.prodid;
				//var cid = value.cid;//�ײ͵���
				var limitdate = "��Ч�� " + value.limitdate;//��Ч��
				//var limittime = "����ʱ�Σ�"+ value.limittime;//��Чʱ��
				var limitday = value.limitday;//ʣ������
				
				var money_class = "money";
				var ticketname_class = "ticketname";
				var ticketinfo_class = "ticketinfo";
				var ticketlimit_class = "normal";
				var line_class = "line";
				var useinfo_class = "useinfoused";
				
				var guoqi = "δʹ��";
				if(state == 1){
					guoqi = "ʹ����";
				}else if(state == 2){
					guoqi = "�ѹ���";
					money_class = "moneyused";
					ticketname_class = "ticketnameused";
					ticketinfo_class = "ticketinfoused";
					ticketlimit_class = "normalused";
					line_class = "lineuesd";
					useinfo_class = "useinfoexp";
				}
				//var click=' onclick="rewand('+prodid+','+cid+')"';
				var click=' onclick="rewand(\''+cardid+'\',\''+prodid+'\',\''+cid+'\')"';
				$("#thelist").append('<li '+click+' class="li1"><div class="moneyouter"><span class="'+money_class+'">'+price+'<span class="fuhao">Ԫ</span></span></div><a class="a1" href="#"><div class="'+ticketname_class+'">'+
						pname+'</div><div class="ticketlimit"><span class="sel_fee '+ticketlimit_class+'">'+cname+'</span></div><div class="ticketlimit2"><span>'+carnumber+'</span></div></a><div class="rewand">����</div></li>');
				$("#thelist").append('<li class="li2"><div class="'+line_class+'"></div><a class="a2" href="#"><div class="'+useinfo_class+'">'+guoqi+'</div><div class="limittime">'+limitdate+'</div></a></li>');
				
			});
			myScroll.refresh(); 
			if(response.length == 0){
				$(".middle").removeClass("hide");
			}
			if (hasMoreData) {
				myScroll.maxScrollY = myScroll.maxScrollY + pullUpOffset;
			} else {
				myScroll.maxScrollY = myScroll.maxScrollY;
			}
			maxScrollY = myScroll.maxScrollY;
		};
	},
	"json");
}

function nextPage() {
	var openid=$("#openid")[0].value;
	page++;
	$.post("carowner.do", {
		"page": page,
		"pagesize": PAGESIZE,
		"openid" : openid,
		"action" : "products"
	},
	function(response, status) {
		if (status == "success") {
			$("#thelist").show();

			if (response.length < PAGESIZE) {
				hasMoreData = false;
				$("#pullUp").hide();
			} else {
				hasMoreData = true;
				$("#pullUp").show();
			}

			// document.getElementById('wrapper').style.left = '0';

			myScroll = new iScroll('wrapper', {
				useTransition: true,
				topOffset: pullDownOffset,
				onRefresh: function() {
					if (pullDownEl.className.match('loading')) {
						pullDownEl.className = 'idle';
						pullDownEl.querySelector('.pullDownLabel').innerHTML = '����ˢ��...';
						this.minScrollY = -pullDownOffset;
					}
					if (pullUpEl.className.match('loading')) {
						pullUpEl.className = 'idle';
						pullUpEl.querySelector('.pullUpLabel').innerHTML = '�������ظ���...';
					}
				},
				onScrollMove: function() {
					if (this.y > OFFSET && !pullDownEl.className.match('flip')) {
						pullDownEl.className = 'flip';
						pullDownEl.querySelector('.pullDownLabel').innerHTML = '���ֿ�ʼˢ��...';
						this.minScrollY = 0;
					} else if (this.y < OFFSET && pullDownEl.className.match('flip')) {
						pullDownEl.className = 'idle';
						pullDownEl.querySelector('.pullDownLabel').innerHTML = '����ˢ��...';
						this.minScrollY = -pullDownOffset;
					} 
					if (this.y < (maxScrollY - pullUpOffset - OFFSET) && !pullUpEl.className.match('flip')) {
						if (hasMoreData) {
							this.maxScrollY = this.maxScrollY - pullUpOffset;
							pullUpEl.className = 'flip';
							pullUpEl.querySelector('.pullUpLabel').innerHTML = '���ֿ�ʼˢ��...';
						}
					} else if (this.y > (maxScrollY - pullUpOffset - OFFSET) && pullUpEl.className.match('flip')) {
						if (hasMoreData) {
							this.maxScrollY = maxScrollY;
							pullUpEl.className = 'idle';
							pullUpEl.querySelector('.pullUpLabel').innerHTML = '�������ظ���...';
						}
					}
				},
				onScrollEnd: function() {
					if (pullDownEl.className.match('flip')) {
						pullDownEl.className = 'loading';
						pullDownEl.querySelector('.pullDownLabel').innerHTML = '������...';
						// pullDownAction(); // Execute custom function (ajax call?)
						refresh();
					}
					if (hasMoreData && pullUpEl.className.match('flip')) {
						pullUpEl.className = 'loading';
						pullUpEl.querySelector('.pullUpLabel').innerHTML = '������...';
						// pullUpAction(); // Execute custom function (ajax call?)
						nextPage();
					}
				}
			});

			$("#thelist").empty();
			$.each(response, function(key, value) {
				var cid = value.cid;//�ƶ��¿���¼���
				var cardid = value.cardid;
				var carnumber = value.carnumber;
				var mid = value.mid;
				var state = value.state;//0��δ��ʼ 1:ʹ���� 2�ѹ���
				var cname = value.name;//ͣ��������
				var pname = value.parkname;//�ײ�����
				var price = value.price;//�ײ͵���
				var prodid = value.prodid;
				//var cid = value.cid;//�ײ͵���
				var limitdate = "��Ч�� " + value.limitdate;//��Ч��
				//var limittime = "����ʱ�Σ�"+ value.limittime;//��Чʱ��
				var limitday = value.limitday;//ʣ������
				
				var money_class = "money";
				var ticketname_class = "ticketname";
				var ticketinfo_class = "ticketinfo";
				var ticketlimit_class = "normal";
				var line_class = "line";
				var useinfo_class = "useinfoused";
				
				var guoqi = "δʹ��";
				if(state == 1){
					guoqi = "ʹ����";
				}else if(state == 2){
					guoqi = "�ѹ���";
					money_class = "moneyused";
					ticketname_class = "ticketnameused";
					ticketinfo_class = "ticketinfoused";
					ticketlimit_class = "normalused";
					line_class = "lineuesd";
					useinfo_class = "useinfoexp";
				}
				//var click=' onclick="rewand('+prodid+','+cid+')"';
				var click=' onclick="rewand(\''+cardid+'\',\''+prodid+'\',\''+cid+'\')"';
				$("#thelist").append('<li '+click+' class="li1"><div class="moneyouter"><span class="'+money_class+'">'+price+'<span class="fuhao">Ԫ</span></span></div><a class="a1" href="#"><div class="'+ticketname_class+'">'+
						pname+'</div><div class="ticketlimit"><span class="sel_fee '+ticketlimit_class+'">'+cname+'</span></div><div class="ticketlimit2"><span>'+carnumber+'</span></div></a><div class="rewand">����</div></li>');
				$("#thelist").append('<li class="li2"><div class="'+line_class+'"></div><a class="a2" href="#"><div class="'+useinfo_class+'">'+guoqi+'</div><div class="limittime">'+limitdate+'</div></a></li>');
				
			});
			myScroll.refresh(); 
			if(response.length == 0){
				$(".middle").removeClass("hide");
			}
			if (hasMoreData) {
				myScroll.maxScrollY = myScroll.maxScrollY + pullUpOffset;
			} else {
				myScroll.maxScrollY = myScroll.maxScrollY;
			}
			maxScrollY = myScroll.maxScrollY;
		};
	},
	"json");
}



//��չDate��format����   
Date.prototype.format = function (format) {  
  var o = {  
      "M+": this.getMonth() + 1,  
      "d+": this.getDate(),  
      "h+": this.getHours(),  
      "m+": this.getMinutes(),  
      "s+": this.getSeconds(),  
      "q+": Math.floor((this.getMonth() + 3) / 3),  
      "S": this.getMilliseconds()  
  }  
  if (/(y+)/.test(format)) {  
      format = format.replace(RegExp.$1, (this.getFullYear() + "").substr(4 - RegExp.$1.length));  
  }  
  for (var k in o) {  
      if (new RegExp("(" + k + ")").test(format)) {  
          format = format.replace(RegExp.$1, RegExp.$1.length == 1 ? o[k] : ("00" + o[k]).substr(("" + o[k]).length));  
      }  
  }  
  return format;  
}

/**   
*ת��longֵΪ�����ַ���   
* @param l longֵ   
* @param isFull �Ƿ�Ϊ��������������,   
*               Ϊtrueʱ, ��ʽ��"2000-03-05 01:05:04"   
*               Ϊfalseʱ, ��ʽ�� "2000-03-05"   
* @return ����Ҫ��������ַ���   
*/    

function getSmpFormatDateByLong(l, isFull) {  
   return getSmpFormatDate(new Date(l), isFull);  
}  

/**   
*ת�����ڶ���Ϊ�����ַ���   
* @param date ���ڶ���   
* @param isFull �Ƿ�Ϊ��������������,   
*               Ϊtrueʱ, ��ʽ��"2000-03-05 01:05:04"   
*               Ϊfalseʱ, ��ʽ�� "2000-03-05"   
* @return ����Ҫ��������ַ���   
*/    
function getSmpFormatDate(date, isFull) {  
    var pattern = "";  
    if (isFull == true || isFull == undefined) {  
        pattern = "yyyy-MM-dd hh:mm:ss";  
    } else {  
        pattern = "yyyy-MM-dd";  
    }  
    return getFormatDate(date, pattern);  
} 

/**   
 *ת�����ڶ���Ϊ�����ַ���   
 * @param l longֵ   
 * @param pattern ��ʽ�ַ���,���磺yyyy-MM-dd hh:mm:ss   
 * @return ����Ҫ��������ַ���   
 */    
 function getFormatDate(date, pattern) {  
     if (date == undefined) {  
         date = new Date();  
     }  
     if (pattern == undefined) {  
         pattern = "yyyy-MM-dd hh:mm:ss";  
     }  
     return date.format(pattern);  
 }
