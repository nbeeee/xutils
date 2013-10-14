importPackage(Packages.zcu.xutil.cfg);
importPackage(Packages.zcu.xutil.web);
importPackage(Packages.zcu.xutil);

new Config(){
	config : function(b) {
		var obj = {
				execute : function(c){return new Stream(".txt",c.getContext().listMe().toString());}
		};
		CFG.val("test").put(b,"testStr");
		CFG.val(new JavaAdapter(Action,obj)).put(b,"action")
	}
}