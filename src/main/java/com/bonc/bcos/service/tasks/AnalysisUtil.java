package com.bonc.bcos.service.tasks;

import com.bonc.bcos.service.model.CmdTablePo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class AnalysisUtil {

	private static final String separator = "\\s+";

	private static final String[] errorLab = new String[]{
			"No resources found"
	};

	//1.解析数据
	public static  CmdTablePo analysisListMap(List<String> results) {
		
		CmdTablePo tablePo = new CmdTablePo();

		List<Integer> headIndex = new ArrayList<>();

		if (results.size()<=0){
			return tablePo;
		}

		// 解析表头
		String headLine = results.get(0);
		//判断filed是否包含在错误结果集中，有则立即返回   如No resources found  代表未查出数据
		for(String lab : errorLab) {
			if(headLine.contains(lab)) {
				tablePo.getFields().add(headLine);
				return tablePo;
			}
		}

		//截取返回数据第一行的filed
		String[] fields=headLine.split(separator);

		//组装字段filed  和 每个字段之间的长度
		List<String> columns = tablePo.getFields();
		int index = 0;
		for (String field : fields) {
			String column = field.trim();
			headIndex.add(headLine.indexOf(column,index));
			columns.add(column);

			// 如果有重复字段 headIndex 将不是自增的 序列
			index = headIndex.get(columns.size()-1)+field.length();
		}

		// 解析标数据
		for(int i=1;i< results.size();i++) {
			String dataLine = results.get(i);

			//2.组装表格字段值
			HashMap<String,String> row = new HashMap<>();
			for(int j=0 ;j<fields.length; j++) {
				int k=j+1;
				String key = tablePo.getFields().get(j);

				if (dataLine.length() <= headIndex.get(j) ){
					//  数据长度比最小值还小
					row.put(key,"");
				} else if (k==fields.length || dataLine.length() <= headIndex.get(k) ){
					// 数据长度勉强够用，或者是最后一个字段
					row.put(key, dataLine.substring(headIndex.get(j)).trim());
				} else {
					// 数据长度充足
					row.put(key, dataLine.substring(headIndex.get(j), headIndex.get(k)).trim());
				}
			}
			tablePo.getRows().add(row);
		}
		return tablePo ;
	}

	//2.解析为list

	public static void main(String[] args) {
		List<String> results = new ArrayList<>(Arrays.asList(
				("NAME                        READY   STATUS    RESTARTS   AGE     IP              NODE            NOMINATED NODE   READINESS GATES\n" +
				"bconsole-86858cf54f-2mgjm   1/1     Running   0          2d19h   192.168.3.56    172-16-11-115   <none>           <none>\n" +
				"bdocs-78754959cc-chnzv      1/1     Running   0          11d     192.168.3.53    172-16-11-115   <none>           <none>\n" +
				"bdos-5567b5ddcd-446zv       1/1     Running   0          81m     192.168.2.145   172-16-11-164   <none>           <none>\n" +
				"blogic-7d5ffb969c-hbsgg     1/1     Running   10         40m     192.168.2.147   172-16-11-164   <none>           <none>\n" +
				"bpm-8557b796f6-rkpjh        1/1     Running   0          11d     192.168.4.8     172-16-11-161   <none>           <none>\n" +
				"cas-6477594ccf-2cbkd        1/1     Running   0          11d     192.168.1.100   172-16-11-134   <none>           <none>\n" +
				"logkit-4sjqf                1/1     Running   0          11d     172.16.11.106   172-16-11-106   <none>           <none>\n" +
				"logkit-9tndp                1/1     Running   0          11d     172.16.11.134   172-16-11-134   <none>           <none>\n" +
				"logkit-bdjm5                1/1     Running   0          11d     172.16.11.164   172-16-11-164   <none>           <none>\n" +
				"logkit-hmbgv                1/1     Running   0          11d     172.16.11.161   172-16-11-161   <none>           <none>\n" +
				"logkit-n58ql                1/1     Running   0          11d     172.16.11.115   172-16-11-115   <none>           <none>\n" +
				"pinpoint-7cb984fc7c-xp468   1/1     Running   0          11d     192.168.4.7     172-16-11-161   <none>           <none>\n" +
				"portal-64b57868df-jxcvg     1/1     Running   0          11d     192.168.3.54    172-16-11-115   <none>           <none>\n" +
				"security-5b5bf986f8-mgl2v   1/1     Running   0          2d19h   192.168.4.10    172-16-11-161   <none>           <none>").split("\n")));

		System.out.println(analysisListMap(results));
	}

}
