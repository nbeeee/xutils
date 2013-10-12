importPackage(Packages.zcu.xutil.cfg);
importPackage(Packages.zcu.xutil.web);
importPackage(Packages.zcu.xutil);

new Config(){
	config : function(b) {
		var obj = {
				context : b.ref("").instance(),
				execute : function(c){obj.context.listMe(); return null;}
		};
		CFG.val("test").put(b,"testStr");
		CFG.val(new JavaAdapter(Action,obj)).put(b,"action")
	}
}