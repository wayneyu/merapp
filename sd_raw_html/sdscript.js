$(document).ready(function (){
$("div.hideable_box>div+div").fadeOut(0);

    // click the title
    // div.hideable_box>div
    // hide the box below it
    // div.hideable_box>div+div
    $(".hideable_box>.box_title").click(function (){
      $(this).siblings().fadeToggle(100);
     });

    $("#latex_box #tags ul li:nth-child(1)").click(function() {   //this will apply to all anchor tags
       $("#latex_box textarea").val($("#latex_box textarea").val()+'$x_1$'); 
    });

    $("#latex_box #tags ul li:nth-child(2)").click(function() {   //this will apply to all anchor tags
       $("#latex_box textarea").val($("#latex_box textarea").val()+'$\\int^4_3 x^2 dx$'); 
    });

    $("#latex_box #tags ul li:nth-child(3)").click(function() {   //this will apply to all anchor tags
       $("#latex_box textarea").val($("#latex_box textarea").val()+'$\\Omega + \\Pi = \\Lambda$'); 
    });
    // $('#latex_box textarea').click(function()  
    // { 
    //    $(this).val($(this).val()+'STOP CLICKING ME!!\n'); 
    // });

    // cmds_latex_box = [
    //     'sx_1$',
    //     'sx_2$',
    //     'sx_3$'
    // ];
        

    // for (i = 0; i < cmds_latex_box.length; i++) {
        // $("#latex_box #tags ul li:nth-child(" + (i+1).toString() + ")").click(function() {   //this will apply to all anchor tags
        //    $("#latex_box textarea").val($("#latex_box textarea").val() + cmds_latex_box[i]); 
        //    console.log(cmds_latex_box[i]);
        // });
    // }

    $("#latex_box button").click(function() {   //this will apply to all anchor tags
       $("#latex_box p").append( $("#latex_box textarea").val() );
       MathJax.Hub.Queue(["Typeset",MathJax.Hub]); // refreshes the view in some sense
       
    });    
});




