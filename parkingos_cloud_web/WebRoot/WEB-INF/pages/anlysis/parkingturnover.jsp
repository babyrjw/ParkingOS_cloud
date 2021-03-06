<%@ page language="java" contentType="text/html; charset=gb2312"
    pageEncoding="gb2312"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=gb2312">
<title>泊位周转率</title>
<link href="css/zTreeStyle1.css" rel="stylesheet" type="text/css">
<link href="css/demo.css" rel="stylesheet" type="text/css">
<link href="css/tq.css" rel="stylesheet" type="text/css">
<link href="css/iconbuttons.css" rel="stylesheet" type="text/css">

<script src="js/tq.js?0817" type="text/javascript">//表格</script>
<script src="js/tq.public.js?0817" type="text/javascript">//表格</script>
<script src="js/tq.datatable.js?0817" type="text/javascript">//表格</script>
<script src="js/tq.form.js?0817" type="text/javascript">//表单</script>
<script src="js/tq.searchform.js?0817" type="text/javascript">//查询表单</script>
<script src="js/tq.window.js?0817" type="text/javascript">//弹窗</script>
<script src="js/tq.hash.js?0817" type="text/javascript">//哈希</script>
<script src="js/tq.stab.js?0817" type="text/javascript">//切换</script>
<script src="js/tq.validata.js?0817" type="text/javascript">//验证</script>
<script src="js/My97DatePicker/WdatePicker.js" type="text/javascript">//日期</script>
<script src="js/jquery.js" type="text/javascript"></script>
<script src="js/jquery.ztree.core-3.5.js" type="text/javascript"></script>
 <script src="js/echarts/echarts.js"></script>
   	<style type="text/css">

   
		.search {
	height: 40px;
	margin: 25px 5px 10px 5px;
	
}
		.qiehua {
	float: left;
	margin-left: 0px;
}
		.qiehua a {
	display:block;
	height: 24px;
    padding:5px line-height:24px;
	color: #5ccdbe;
	letter-spacing: 0.2em;
	width: 60px;
	text-align: center;
	font-size: 16px;
	float: left;
	margin-left: 0px;
	background: #fff;
	border: #5ccdbe 1px solid;
}
	</style>

</head>
<body>
<div class="search">
        <div class="qiehua">
        <a  href=""  id="table">列表</a>
        <a href="parkingturnover.do?action=echarts" id="icon"  >图表</a>
        </div>
         <iframe src="" id ="exportiframe" frameborder="0" style="width:0px;height:0px;"></iframe>
        
        <form action="" method="get">
          <div class="an">
            
          </div>
          <!--搜索按钮-->
          <div class="kuan2">
           
          </div>
          <!--搜索框-->
        </form>
</div >
<div id="parkingturnoverobj" style="width:100%;height:100%;margin:0px;"></div>

<script type="text/javascript" >
      $(function(){
        
        	$("#table").css('background','#5ccdbe').css('color','#fff');
        });
        
var btime="${btime}";
var etime="${etime}";
//var etime="${etime}";
var _mediaField = [
		{fieldcnname:"车场编号",fieldname:"comid",inputtype:"text", twidth:"100" ,issort:false},
		{fieldcnname:"停车场",fieldname:"company_name",inputtype:"text", twidth:"200",issort:false},
		{fieldcnname:"停车次数",fieldname:"parktimes",inputtype:"number", twidth:"200",issort:false},
		{fieldcnname:"泊位数",fieldname:"parking_total",inputtype:"number", twidth:"200",issort:false},
		{fieldcnname:"周转次数",fieldname:"parkingturn",inputtype:"number", twidth:"200",issort:false}
		];
var _anlysisT = new TQTable({
	tabletitle:"泊位周转率",
	ischeck:false,
	tablename:"parkingturnover_tables",
	dataUrl:"parkingturnover.do",
	iscookcol:false,
	buttons:false,
	quikcsearch:coutomsearch(),
	param:"action=query",
	tableObj:T("#parkingturnoverobj"),
	fit:[true,true,true],
	tableitems:_mediaField,
	allowpage:false,
	buttons:getAuthButtons(),
	isoperate:getAuthIsoperateButtons()
});

function coutomsearch(){
/* 	var html = "&nbsp;&nbsp; 时间：<input id='coutom_btime' value='"+btime+"' style='width:70px' onClick=\"WdatePicker({dateFmt:'yyyy-MM-dd',startDate:'%y-%M-01',alwaysUseStartDate:true});\"/>"+
				"&nbsp;&nbsp;<input type='button' onclick='searchdata();' value=' 查 询 '/>"; */
				
	var html = "&nbsp;&nbsp;&nbsp;&nbsp;时间：&nbsp;&nbsp;<input id='coutom_btime' class='Wdate' align='absmiddle' readonly value='"+btime+"' style='width:150px' onClick=\"WdatePicker({dateFmt:'yyyy-MM-dd HH:mm:ss',startDate:'%y-%M-%d 00:00:00',alwaysUseStartDate:false});\"/>"
				+" - <input id='coutom_etime' class='Wdate' align='absmiddle' readonly value='"+etime+"' style='width:150px' onClick=\"WdatePicker({dateFmt:'yyyy-MM-dd HH:mm:ss',startDate:'%y-%M-%d 23:59:59',alwaysUseStartDate:false});\"/>"
				+ "&nbsp;&nbsp;<input type='button' onclick='searchdata();' value=' 查 询 '/>";
				return html;
				
	return html;
}

function searchdata(){
    
	btime = T("#coutom_btime").value;

	//data=eval(T.A.sendData("parkingturnover.do?action=echarts&btime="+btime+"&etime="+etime));
	_anlysisT.C({
		cpage:1,
		tabletitle:"搜索结果",
		extparam:"&action=query&btime="+btime,
	});
	T("#coutom_btime").value=btime;

   
}
function getAuthButtons(){
	var bts=[];
	//if(subauth[0])
		bts.push({dname:"导出报表",icon:"toxls.gif",onpress:function(Obj){
			btime = T("#coutom_btime").value;
			T("#exportiframe").src="parkingturnover.do?action=export&btime="+btime;
			T.loadTip(1,"正在导出，请稍候...",2,"");
		}});
	return bts;
}
function getAuthIsoperateButtons(){
	var bts = [];
	if(bts.length <= 0){return false;}
	return bts;
}
_anlysisT.C();

</script>
</body>
</html>
