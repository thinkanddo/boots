package com.bonc.bcos.service.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CmdDictPo {

	private CmdStdoutPo stdoutPo = new CmdStdoutPo();
	
    private List<String> rows = new ArrayList<>();
    
}
