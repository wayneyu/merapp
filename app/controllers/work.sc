import models.Question

val qid = "MATH220+April_2011+1\\s(a)"
Question.parseQid(qid)



	.on("select2:select", function(e){
	var newTag = e.params.data.id;
	console.log(newTag);
	$.ajax({
		type: 'GET',
		url: window.location.pathname + "/topics/add/" + newTag,
		success: function(d){
			console.log("added new tag");
		},
		error: function(obj, st, err){
			alert(err + "\n" + obj.responseText);
		}
	})
})
	.on("select2:unselect",function(e){
	var removedTag = e.params.data.id;
	console.log(removedTag);
	$.ajax({
		type: 'GET',
		url: window.location.pathname + "/topics/remove/" + removedTag,
		success: function(d){
			console.log("removed tag");
		},
		error: function(obj, st, err){
			alert(err + "\n" + obj.responseText);
		}
	})
});