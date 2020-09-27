package com.bonc.bcos.service.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Data
public class CmdTablePo {
	private CmdStdoutPo stdoutPo;
	
    private List<String> fields = new ArrayList<>();

    private List<HashMap<String,String>> rows = new ArrayList<>();

}
