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
       //$("#latex_box textarea").val($("#latex_box textarea").val()+'$x_1$'); 
       $("#latex_box textarea").insertAtCaret('$x_{1}$'); 
    });

    $("#latex_box #tags ul li:nth-child(2)").click(function() {   //this will apply to all anchor tags
       $("#latex_box textarea").insertAtCaret('$\\int_{a}^{b} f(x)\\,dx$'); 
    });

    $("#latex_box #tags ul li:nth-child(3)").click(function() {   //this will apply to all anchor tags
       $("#latex_box textarea").insertAtCaret('$\\alpha$'); 
    });

    $("#latex_box #tags ul li:nth-child(4)").click(function() {   //this will apply to all anchor tags
       $("#latex_box textarea").insertAtCaret('$\\frac{d}{dx}$'); 
    });

    $("#latex_box #tags ul li:nth-child(5)").click(function() {   //this will apply to all anchor tags
       $("#latex_box textarea").insertAtCaret('<font color="blue"> blue text </font>')
    });

    $("#latex_box #tags ul li:nth-child(6)").click(function() {   //this will apply to all anchor tags
       $("#latex_box textarea").insertAtCaret('\n\\begin{equation}\n f(x) = g(x) + a \n\\end{equation} \n')
    });

    $("#latex_box button").click(function() {   //this will apply to all anchor tags
       $("#latex_box p").empty(); 
       $("#latex_box p").append( $("#latex_box textarea").val() );
       MathJax.Hub.Queue(["Typeset",MathJax.Hub]); // refreshes the view in some sense
       
    });    
});

$.fn.insertAtCaret = function (tagName) {
  return this.each(function(){
    if (document.selection) {
      //IE support
      this.focus();
      sel = document.selection.createRange();
      sel.text = tagName;
      this.focus();
    }else if (this.selectionStart || this.selectionStart == '0') {
      //MOZILLA/NETSCAPE support
      startPos = this.selectionStart;
      endPos = this.selectionEnd;
      scrollTop = this.scrollTop;
      this.value = this.value.substring(0, startPos) + tagName + this.value.substring(endPos,this.value.length);
      this.focus();
      this.selectionStart = startPos + tagName.length;
      this.selectionEnd = startPos + tagName.length;
      this.scrollTop = scrollTop;
    } else {
      this.value += tagName;
      this.focus();
    }
  });
};


