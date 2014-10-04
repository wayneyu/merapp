$(document).ready(function (){
$("div.hideable_box>div+div").fadeOut(0);

    // click the title
    // div.hideable_box>div
    // hide the box below it
    // div.hideable_box>div+div
    $(".box_title").click(function (){
      $(this).siblings().fadeToggle(100);
     });

  //   console.log("Script included!");
  //   console.log(typeof "sd string");
  //   console.log(typeof $);

  //   $('#ingredients tbody tr').click(function (){
  //   $(this).css( {"color":"grey","transition":"color .5s"});

  //   });

  // // $("li.skip").insertAfter("li:first-child");
  //   $('ol li').click(function (){
  //   $("li.skip, p").insertAfter(this);

  //   });

  //   $('#magicButton').click(function (){
  //     $("#images img").fadeOut(1500, function () {
  //      $(this).attr("src","http://upload.wikimedia.org/wikipedia/commons/a/aa/Empty_set.svg"); 

  //     } )
  //   .fadeIn(500)

  //   ;


    

  //   // DOESNT WORK 
  //   // http://jsfiddle.net/mblase75/fa5Wn/
  //   $("#ingredients::after").toggleClass("special");
  //   });


});







